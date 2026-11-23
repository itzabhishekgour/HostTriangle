/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * HostTriangle - Distributed Edge Compute Nodes for Android
 * Copyright (C) 2026 Abhishek Gour (Tinexus)
 *
 * This file is part of HostTriangle.
 *
 * HostTriangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#define _GNU_SOURCE
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <dirent.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <android/log.h>

#define LOG_TAG "HostTrianglePTY"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern char **environ;

static void raise_java_exception(JNIEnv *env, const char *message) {
    jclass ex_cls = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (ex_cls != NULL) {
        (*env)->ThrowNew(env, ex_cls, message);
    }
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_createSubprocess(
    JNIEnv *env,
    jclass clazz,
    jstring cmd,
    jstring cwd,
    jobjectArray args,
    jobjectArray envVars,
    jintArray processIdArray,
    jint rows,
    jint cols) {

    // 1. Open master PTY descriptor
    int master_fd = posix_openpt(O_RDWR | O_NOCTTY | O_CLOEXEC);
    if (master_fd < 0) {
        char err_buf[128];
        snprintf(err_buf, sizeof(err_buf), "posix_openpt failed: %s", strerror(errno));
        raise_java_exception(env, err_buf);
        return -1;
    }

    if (grantpt(master_fd) != 0) {
        close(master_fd);
        raise_java_exception(env, "grantpt failed");
        return -1;
    }

    if (unlockpt(master_fd) != 0) {
        close(master_fd);
        raise_java_exception(env, "unlockpt failed");
        return -1;
    }

    char slave_dev[128];
    if (ptsname_r(master_fd, slave_dev, sizeof(slave_dev)) != 0) {
        close(master_fd);
        raise_java_exception(env, "ptsname_r failed");
        return -1;
    }

    // 2. Set terminal attributes: enable IUTF8, disable software flow control (IXON/IXOFF)
    struct termios ti;
    if (tcgetattr(master_fd, &ti) == 0) {
#ifdef IUTF8
        ti.c_iflag |= IUTF8;
#endif
        ti.c_iflag &= ~(IXON | IXOFF);
        tcsetattr(master_fd, TCSANOW, &ti);
    }

    // 3. Set initial terminal window size
    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short)(rows > 0 ? rows : 24);
    ws.ws_col = (unsigned short)(cols > 0 ? cols : 80);
    ioctl(master_fd, TIOCSWINSZ, &ws);

    // 4. Extract strings for cmd and cwd
    const char *cmd_utf = (*env)->GetStringUTFChars(env, cmd, NULL);
    if (!cmd_utf) {
        close(master_fd);
        raise_java_exception(env, "Failed to get command string");
        return -1;
    }

    const char *cwd_utf = NULL;
    if (cwd != NULL) {
        cwd_utf = (*env)->GetStringUTFChars(env, cwd, NULL);
    }

    // 5. Build argument array (argv)
    int argc = 0;
    if (args != NULL) {
        argc = (*env)->GetArrayLength(env, args);
    }

    char **argv = NULL;
    if (argc > 0) {
        argv = (char **)calloc(argc + 1, sizeof(char *));
        if (!argv) {
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
            if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
            close(master_fd);
            raise_java_exception(env, "Out of memory allocating argv");
            return -1;
        }
        for (int i = 0; i < argc; i++) {
            jstring arg_item = (jstring)(*env)->GetObjectArrayElement(env, args, i);
            if (arg_item != NULL) {
                const char *arg_str = (*env)->GetStringUTFChars(env, arg_item, NULL);
                argv[i] = strdup(arg_str ? arg_str : "");
                if (arg_str) (*env)->ReleaseStringUTFChars(env, arg_item, arg_str);
                (*env)->DeleteLocalRef(env, arg_item);
            } else {
                argv[i] = strdup("");
            }
        }
        argv[argc] = NULL;
    } else {
        argv = (char **)calloc(2, sizeof(char *));
        if (!argv) {
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
            if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
            close(master_fd);
            raise_java_exception(env, "Out of memory allocating argv");
            return -1;
        }
        argv[0] = strdup(cmd_utf);
        argv[1] = NULL;
    }

    // 6. Build environment array (envp)
    int envc = 0;
    if (envVars != NULL) {
        envc = (*env)->GetArrayLength(env, envVars);
    }

    char **envp = NULL;
    if (envc > 0) {
        envp = (char **)calloc(envc + 1, sizeof(char *));
        if (envp) {
            for (int i = 0; i < envc; i++) {
                jstring env_item = (jstring)(*env)->GetObjectArrayElement(env, envVars, i);
                if (env_item != NULL) {
                    const char *env_str = (*env)->GetStringUTFChars(env, env_item, NULL);
                    envp[i] = strdup(env_str ? env_str : "");
                    if (env_str) (*env)->ReleaseStringUTFChars(env, env_item, env_str);
                    (*env)->DeleteLocalRef(env, env_item);
                } else {
                    envp[i] = strdup("");
                }
            }
            envp[envc] = NULL;
        }
    }

    // 7. Fork child process
    pid_t pid = fork();
    if (pid < 0) {
        int err = errno;
        for (int i = 0; argv[i] != NULL; i++) free(argv[i]);
        free(argv);
        if (envp) {
            for (int i = 0; envp[i] != NULL; i++) free(envp[i]);
            free(envp);
        }
        (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
        if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
        close(master_fd);

        char err_buf[128];
        snprintf(err_buf, sizeof(err_buf), "fork failed: %s", strerror(err));
        raise_java_exception(env, err_buf);
        return -1;
    }

    if (pid == 0) {
        // --- CHILD PROCESS ---
        // Establish independent session and process group
        setsid();

        // Open slave side of PTY
        int slave_fd = open(slave_dev, O_RDWR);
        if (slave_fd < 0) {
            _exit(1);
        }

        // Set slave PTY as controlling terminal
        ioctl(slave_fd, TIOCSCTTY, 0);

        // Redirect standard file descriptors to slave PTY
        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);

        if (slave_fd > STDERR_FILENO) {
            close(slave_fd);
        }
        if (master_fd >= 0) {
            close(master_fd);
        }

        // Close lingering inherited file descriptors safely via /proc/self/fd
        DIR *dir = opendir("/proc/self/fd");
        if (dir != NULL) {
            int dir_fd = dirfd(dir);
            struct dirent *entry;
            while ((entry = readdir(dir)) != NULL) {
                char *endptr = NULL;
                long fd = strtol(entry->d_name, &endptr, 10);
                if (endptr != entry->d_name && *endptr == '\0') {
                    if (fd > STDERR_FILENO && fd != dir_fd) {
                        close((int)fd);
                    }
                }
            }
            closedir(dir);
        } else {
            int max_fd = (int)sysconf(_SC_OPEN_MAX);
            if (max_fd < 32 || max_fd > 1024) {
                max_fd = 256;
            }
            for (int fd = 3; fd < max_fd; fd++) {
                close(fd);
            }
        }

        // Unblock all signals so JVM thread signal masks do not starve child
        sigset_t empty_mask;
        sigemptyset(&empty_mask);
        sigprocmask(SIG_SETMASK, &empty_mask, NULL);

        // Reset signal handlers to default
        signal(SIGHUP, SIG_DFL);
        signal(SIGINT, SIG_DFL);
        signal(SIGQUIT, SIG_DFL);
        signal(SIGTERM, SIG_DFL);
        signal(SIGCHLD, SIG_DFL);
        signal(SIGPIPE, SIG_DFL);

        // Change directory if requested
        if (cwd_utf != NULL && cwd_utf[0] != '\0') {
            if (chdir(cwd_utf) != 0) {
                // directory fallback
            }
        }

        // Set environment and execute binary
        if (envp != NULL) {
            environ = envp;
            execvpe(cmd_utf, argv, envp);
            execve(cmd_utf, argv, envp);
            execvp(cmd_utf, argv);
        } else {
            execvp(cmd_utf, argv);
        }

        _exit(127);
    }

    // --- PARENT PROCESS ---
    if (argv) {
        for (int i = 0; argv[i] != NULL; i++) {
            free(argv[i]);
        }
        free(argv);
    }

    if (envp) {
        for (int i = 0; envp[i] != NULL; i++) {
            free(envp[i]);
        }
        free(envp);
    }

    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
    if (cwd_utf) {
        (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
    }

    if (processIdArray != NULL && (*env)->GetArrayLength(env, processIdArray) > 0) {
        jint child_pid = (jint)pid;
        (*env)->SetIntArrayRegion(env, processIdArray, 0, 1, &child_pid);
    }

    return (jint)master_fd;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyWindowSize(
    JNIEnv *env,
    jclass clazz,
    jint fd,
    jint rows,
    jint cols) {

    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short)(rows > 0 ? rows : 24);
    ws.ws_col = (unsigned short)(cols > 0 ? cols : 80);
    ioctl((int)fd, TIOCSWINSZ, &ws);
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_waitFor(
    JNIEnv *env,
    jclass clazz,
    jint pid) {

    int status = 0;
    pid_t res;
    do {
        res = waitpid((pid_t)pid, &status, 0);
    } while (res == -1 && errno == EINTR);

    if (res == -1) {
        return -errno;
    }

    if (WIFEXITED(status)) {
        return (jint)WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        return (jint)(-WTERMSIG(status));
    }
    return 0;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_close(
    JNIEnv *env,
    jclass clazz,
    jint fd) {

    if (fd >= 0) {
        close((int)fd);
    }
}
