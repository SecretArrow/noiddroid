// jni_bridge.cpp — JNI entry points for the Kotlin layer.
//
// Forwards Kotlin calls to the Rust core (libnoiddroid_core.a) and to the
// Vulkan compute shim (vulkan_compute.cpp). The JNI signatures must match
// the `external fun` declarations in `org.secretarrow.noiddroid.mine.MinerNative`.

#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <memory>

// ── Rust core FFI (linked via libnoiddroid_core.a) ──────────────────────
extern "C" {
    typedef struct NoidMiner NoidMiner;
    typedef struct NoidTemplate {
        uint8_t header[80];
        uint8_t target[32];
        uint64_t template_id;
    } NoidTemplate;
    typedef struct NoidSolution {
        uint64_t template_id;
        uint64_t nonce_lo;
        uint64_t nonce_hi;
        uint64_t hash_rate;
    } NoidSolution;

    NoidMiner* noid_miner_create(int threads);
    void noid_miner_destroy(NoidMiner* m);
    void noid_miner_stop(NoidMiner* m);
    uint64_t noid_miner_hashrate(const NoidMiner* m);
    int noid_search_cpu(NoidMiner* m,
                        const NoidTemplate* tpl,
                        NoidSolution* sol,
                        void (*progress)(uint64_t rate, void* ctx),
                        void* ctx);
    int noid_poseidon2b_hash(const uint8_t* input, size_t len, uint8_t* out, size_t out_len);
}

// ── Vulkan compute shim (vulkan_compute.cpp) ───────────────────────────
extern "C" int noid_search_gpu(const NoidTemplate* tpl,
                               NoidSolution* sol,
                               void (*progress)(uint64_t rate, void* ctx),
                               void* ctx);

#define TAG "NoiddroidNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

// Persistent miner pointer — Kotlin treats this as opaque long.
JNIEXPORT jlong JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativeMinerCreate(
        JNIEnv* env, jclass klass, jint threads) {
    (void)env; (void)klass;
    auto* m = noid_miner_create(static_cast<int>(threads));
    if (!m) {
        LOGE("nativeMinerCreate: noid_miner_create returned null");
        return 0;
    }
    return reinterpret_cast<jlong>(m);
}

JNIEXPORT void JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativeMinerDestroy(
        JNIEnv* env, jclass klass, jlong handle) {
    (void)env; (void)klass;
    auto* m = reinterpret_cast<NoidMiner*>(handle);
    if (m) noid_miner_destroy(m);
}

JNIEXPORT void JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativeMinerStop(
        JNIEnv* env, jclass klass, jlong handle) {
    (void)env; (void)klass;
    auto* m = reinterpret_cast<NoidMiner*>(handle);
    if (m) noid_miner_stop(m);
}

JNIEXPORT jlong JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativeMinerHashrate(
        JNIEnv* env, jclass klass, jlong handle) {
    (void)env; (void)klass;
    auto* m = reinterpret_cast<const NoidMiner*>(handle);
    return m ? noid_miner_hashrate(m) : 0;
}

// JNI type for the Kotlin progress callback.
struct ProgressCallbackCtx {
    JavaVM* vm;
    jobject global_ref;
    jmethodID mid;
};

static void forward_progress(uint64_t rate, void* ctx) {
    auto* c = static_cast<ProgressCallbackCtx*>(ctx);
    if (!c) return;
    JNIEnv* env = nullptr;
    bool attached = false;
    if (c->vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (c->vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        attached = true;
    }
    env->CallVoidMethod(c->global_ref, c->mid, static_cast<jlong>(rate));
    if (attached) c->vm->DetachCurrentThread();
}

JNIEXPORT jint JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativeSearchCpu(
        JNIEnv* env, jclass klass,
        jlong handle,
        jbyteArray headerArr,
        jbyteArray targetArr,
        jlong templateId,
        jobject progressCallback,
        jlongArray solutionArr) {
    (void)klass;

    auto* m = reinterpret_cast<NoidMiner*>(handle);
    if (!m) {
        LOGE("nativeSearchCpu: null miner handle");
        return -1;
    }
    if (env->GetArrayLength(headerArr) != 80) {
        LOGE("nativeSearchCpu: header array length != 80");
        return -2;
    }
    if (env->GetArrayLength(targetArr) != 32) {
        LOGE("nativeSearchCpu: target array length != 32");
        return -3;
    }

    NoidTemplate tpl{};
    env->GetByteArrayRegion(headerArr, 0, 80, reinterpret_cast<jbyte*>(tpl.header));
    env->GetByteArrayRegion(targetArr, 0, 32,  reinterpret_cast<jbyte*>(tpl.target));
    tpl.template_id = static_cast<uint64_t>(templateId);

    NoidSolution sol{};

    ProgressCallbackCtx ctx{};
    if (progressCallback) {
        JNIEnv* e = env;
        JavaVM* vm = nullptr;
        e->GetJavaVM(&vm);
        ctx.vm = vm;
        ctx.global_ref = e->NewGlobalRef(progressCallback);
        jclass cbClass = e->GetObjectClass(progressCallback);
        ctx.mid = e->GetMethodID(cbClass, "onProgress", "(J)V");
        e->DeleteLocalRef(cbClass);
    }

    int rc = noid_search_cpu(
        m, &tpl, &sol,
        progressCallback ? &forward_progress : nullptr,
        progressCallback ? &ctx : nullptr);

    if (progressCallback && ctx.global_ref) {
        env->DeleteGlobalRef(ctx.global_ref);
    }

    // Pack solution into the long array (templateId, nonce_lo, nonce_hi, hash_rate).
    if (env->GetArrayLength(solutionArr) >= 4) {
        jlong buf[4] = {
            static_cast<jlong>(sol.template_id),
            static_cast<jlong>(sol.nonce_lo),
            static_cast<jlong>(sol.nonce_hi),
            static_cast<jlong>(sol.hash_rate),
        };
        env->SetLongArrayRegion(solutionArr, 0, 4, buf);
    }
    return static_cast<jint>(rc);
}

JNIEXPORT jint JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativeSearchGpu(
        JNIEnv* env, jclass klass,
        jbyteArray headerArr,
        jbyteArray targetArr,
        jlong templateId,
        jobject progressCallback,
        jlongArray solutionArr) {
    (void)klass;

    NoidTemplate tpl{};
    if (env->GetArrayLength(headerArr) != 80 || env->GetArrayLength(targetArr) != 32) {
        return -1;
    }
    env->GetByteArrayRegion(headerArr, 0, 80, reinterpret_cast<jbyte*>(tpl.header));
    env->GetByteArrayRegion(targetArr, 0, 32,  reinterpret_cast<jbyte*>(tpl.target));
    tpl.template_id = static_cast<uint64_t>(templateId);

    NoidSolution sol{};

    ProgressCallbackCtx ctx{};
    if (progressCallback) {
        JavaVM* vm = nullptr;
        env->GetJavaVM(&vm);
        ctx.vm = vm;
        ctx.global_ref = env->NewGlobalRef(progressCallback);
        jclass cbClass = env->GetObjectClass(progressCallback);
        ctx.mid = env->GetMethodID(cbClass, "onProgress", "(J)V");
        env->DeleteLocalRef(cbClass);
    }

    int rc = noid_search_gpu(
        &tpl, &sol,
        progressCallback ? &forward_progress : nullptr,
        progressCallback ? &ctx : nullptr);

    if (progressCallback && ctx.global_ref) {
        env->DeleteGlobalRef(ctx.global_ref);
    }

    if (env->GetArrayLength(solutionArr) >= 4) {
        jlong buf[4] = {
            static_cast<jlong>(sol.template_id),
            static_cast<jlong>(sol.nonce_lo),
            static_cast<jlong>(sol.nonce_hi),
            static_cast<jlong>(sol.hash_rate),
        };
        env->SetLongArrayRegion(solutionArr, 0, 4, buf);
    }
    return static_cast<jint>(rc);
}

JNIEXPORT jbyteArray JNICALL
Java_org_secretarrow_noiddroid_mine_MinerNative_nativePoseidon2bHash(
        JNIEnv* env, jclass klass, jbyteArray inputArr) {
    (void)klass;
    jsize n = env->GetArrayLength(inputArr);
    auto* buf = new (std::nothrow) uint8_t[n];
    if (!buf) return nullptr;
    env->GetByteArrayRegion(inputArr, 0, n, reinterpret_cast<jbyte*>(buf));

    uint8_t out[32];
    int rc = noid_poseidon2b_hash(buf, static_cast<size_t>(n), out, sizeof(out));
    delete[] buf;
    if (rc != 0) return nullptr;

    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, reinterpret_cast<jbyte*>(out));
    return result;
}

// Called by System.loadLibrary("noiddroid") — log a one-liner.
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    LOGI("noiddroid native module loaded");
    return JNI_VERSION_1_6;
}

} // extern "C"
