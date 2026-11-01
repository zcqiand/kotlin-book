# -*- coding: utf-8 -*-
"""ch08 实测回执逐字节比对：实测落盘文件 vs 正文清单期望值。"""
import json, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

BASE = r"D:/qiand-life/1-projects/xr-knowledge-suite/output/xr-know-020/chapters/_work/lab-ch08"
R = BASE + "/receipts"

# 期望值：逐字取自 chapter-008.md 清单 8-5 / 8-6 / 8-7（仅程序输出行）
EXPECTED = {
    "r01_noargs.txt": {
        "listing": "8-5 发①",
        "lines": ["用法：todo-cli <命令> [参数]",
                  "  add <内容>   新增一条待办",
                  "  list         列出全部待办并统计",
                  "  done <编号>  标记指定待办为已完成",
                  "  rm <编号>    删除指定待办"],
    },
    "r02_list_empty.txt": {"listing": "8-5 发②", "lines": ["清单还是空的，先用 add 添加一条试试"]},
    "r03_add1.txt":       {"listing": "8-5 发③", "lines": ["已添加 #1：买牛奶"]},
    "r04_add2.txt":       {"listing": "8-5 发④", "lines": ["已添加 #2：写周报"]},
    "r05_add3.txt":       {"listing": "8-5 发⑤", "lines": ["已添加 #3：订周一的会议室"]},
    "r06_list3.txt":      {"listing": "8-5 发⑥", "lines": ["[ ] #1 买牛奶",
                  "[ ] #2 写周报",
                  "[ ] #3 订周一的会议室",
                  "共 3 条：未完成 3，已完成 0"]},
    "r07_done2.txt":      {"listing": "8-6 发⑦", "lines": ["已完成 #2"]},
    "r08_rm1.txt":        {"listing": "8-6 发⑧", "lines": ["已删除 #1"]},
    "r09_done9.txt":      {"listing": "8-6 发⑨", "lines": ["找不到 #9，清单未改动"]},
    "r10_undo.txt":       {"listing": "8-6 发⑩", "lines": ["未知命令：undo（可用：add / list / done / rm）"]},
    "r11_list_final.txt": {"listing": "8-6 终态 list", "lines": ["[x] #2 写周报",
                  "[ ] #3 订周一的会议室",
                  "共 2 条：未完成 1，已完成 1"]},
    "todos.json":         {"listing": "8-7 终态", "lines": ['{"id":2,"done":true,"text":"写周报"}',
                  '{"id":3,"done":false,"text":"订周一的会议室"}']},
}

results = []
for fname, exp in EXPECTED.items():
    path = (BASE + "/todo-cli/" + fname) if fname == "todos.json" else (R + "/" + fname)
    raw = open(path, "rb").read()
    actual = raw.decode("utf-8")
    # 归一 CRLF/LF 后按行比（Windows lineSeparator 落盘为 CRLF，属环境差异，单独记录）
    actual_lines = actual.replace("\r\n", "\n").split("\n")
    if actual_lines and actual_lines[-1] == "":
        actual_lines = actual_lines[:-1]  # 去掉文件末尾换行产生的空尾
    diffs = []
    for i, (a, e) in enumerate(zip(actual_lines, exp["lines"])):
        if a != e:
            diffs.append("line %d: actual=%r expected=%r" % (i + 1, a, e))
    if len(actual_lines) != len(exp["lines"]):
        diffs.append("line count: actual=%d expected=%d" % (len(actual_lines), len(exp["lines"])))
    had_crlf = b"\r\n" in raw
    results.append({"file": fname, "listing": exp["listing"], "match": not diffs,
                    "crlf_on_disk": had_crlf, "diffs": diffs})

ok = all(r["match"] for r in results)
print(json.dumps(results, ensure_ascii=False, indent=1))
print("VERDICT:", "all_match" if ok else "diffs_found")
