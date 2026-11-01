# -*- coding: utf-8 -*-
"""ch10 open-philosophy 回执采集器：原样落盘，禁美化禁截改。
用法：
  python run_gradle.py run <subcmd> <outfile>      # gradle run --args=<subcmd>，stdout 存 outfile
  python run_gradle.py compile-broken <kt-name> <outfile>  # 把 broken/<kt> 拷进 src 触发编译失败，合并输出存 outfile
"""
import os
import shutil
import subprocess
import sys

PROJ = os.path.dirname(os.path.abspath(__file__))
SRC_DEMO = os.path.join(PROJ, "src", "main", "kotlin", "demo")
BROKEN = os.path.join(PROJ, "broken")
RECEIPTS = os.path.join(PROJ, "receipts")
# 坑 F16'：中文回执必须显式 UTF-8，否则 Windows 默认 GBK 乱码，回执作废
ENV = dict(os.environ, JAVA_TOOL_OPTIONS="-Dstdout.encoding=UTF-8")


def gradle(gargs):
    # gradle 实为 gradle.bat，CreateProcess 找不到裸名，须走 shell
    cmd = "gradle --console=plain " + gargs
    return subprocess.run(cmd, cwd=PROJ, env=ENV, shell=True,
                          capture_output=True, text=True, encoding="utf-8", errors="replace")


def run_demo(subcmd, outfile):
    p = gradle("run --args=" + subcmd)
    out = p.stdout
    if p.returncode != 0 or "�" in out:
        # 失败或乱码时连 stderr 一起交出，方便定位，不静默吞掉
        out += "\n[STDERR]\n" + p.stderr
    with open(os.path.join(RECEIPTS, outfile), "w", encoding="utf-8", newline="") as f:
        f.write(out)
    print(f"rc={p.returncode} -> receipts/{outfile}")


def compile_broken(kt, outfile):
    src = os.path.join(SRC_DEMO, kt)
    dst = os.path.join(BROKEN, kt)
    shutil.copy(dst, src)  # 临时把反例放进编译范围
    try:
        p = gradle("compileKotlin")
        # 失败回执保完整：stdout+stderr 原样合并，一行不删
        text = p.stdout + p.stderr
    finally:
        os.remove(src)  # 无论成败都移出编译范围，保证正发工程可独立编译
    with open(os.path.join(RECEIPTS, outfile), "w", encoding="utf-8", newline="") as f:
        f.write(text)
    print(f"rc={p.returncode} -> receipts/{outfile}")


if __name__ == "__main__":
    os.makedirs(RECEIPTS, exist_ok=True)
    action = sys.argv[1]
    if action == "run":
        run_demo(sys.argv[2], sys.argv[3])
    else:
        compile_broken(sys.argv[2], sys.argv[3])
