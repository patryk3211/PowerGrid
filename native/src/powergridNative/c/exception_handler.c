#include <setjmp.h>
#include <exception_handler.h>

jmp_buf jump_buffer;

#ifdef _WIN32
#include <windows.h>

  static PVOID handler_ptr;

  static LONG WINAPI windows_exception_handler(EXCEPTION_POINTERS *ExceptionInfo) {
    if(ExceptionInfo->ExceptionRecord->ExceptionCode == EXCEPTION_ILLEGAL_INSTRUCTION) {
      longjmp(jump_buffer, 1);
    }

    return EXCEPTION_CONTINUE_SEARCH;
  }

  static int set_signal_handler() {
    handler_ptr = AddVectoredExceptionHandler(1, windows_exception_handler);
    return 1;
  }

  static void remove_signal_handler() {
    RemoveVectoredExceptionHandler(handler_ptr);
  }
#else
#include <signal.h>
#include <stdint.h>
#include <stdlib.h>

  static void posix_signal_handler(int sig, siginfo_t *siginfo, void *context) {
    if(sig == SIGILL) {
      longjmp(jump_buffer, 1);
    }
  }

  static stack_t old_stack;
  static struct sigaction old_action;
  static void *alternate_stack;
  static int set_signal_handler() {
    stack_t ss = {};
    alternate_stack = malloc(SIGSTKSZ);
    ss.ss_sp = alternate_stack;
    ss.ss_size = SIGSTKSZ;
    ss.ss_flags = 0;

    if(sigaltstack(&ss, &old_stack) != 0) {
      return 0;
    }

    struct sigaction sig_action = {};
    sig_action.sa_sigaction = posix_signal_handler;
    sigemptyset(&sig_action.sa_mask);
    sig_action.sa_flags = SA_SIGINFO | SA_ONSTACK;
    if(sigaction(SIGILL, &sig_action, &old_action) != 0) {
      return 0;
    }
    return 1;
  }

  static void remove_signal_handler() {
    sigaction(SIGILL, &old_action, NULL);
    sigaltstack(&old_stack, NULL);
    free(alternate_stack);
  }
#endif

int powergrid_run_safely(int (*func)()) {
    int result = 0;
    if(!set_signal_handler()) {
      result = -1;
    } else {
      if(setjmp(jump_buffer)) {
        // Execution failed.
        result = -1;
      } else {
        result = func();
      }
    }
    remove_signal_handler();
    return result;
}

