// Java：字段、构造器、访问器，各写各的
public class Progress {
    private final int percent;

    public Progress(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent 必须在 0..100：" + percent);
        }
        this.percent = percent;
    }

    public int getPercent() {
        return percent;
    }

    public boolean isDone() {
        return percent == 100;
    }
}