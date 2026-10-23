package jxmvc.core;

public final class JxPageResult {
    private final DBRowSet data;
    private final int      page;
    private final int      size;
    private final long     total;
    private final int      totalPages;

    private JxPageResult(DBRowSet data, int page, int size, long total, int totalPages) {
        this.data       = data;
        this.page       = page;
        this.size       = size;
        this.total      = total;
        this.totalPages = totalPages;
    }

    public static JxPageResult of(DBRowSet data, int page, int size, long total) {
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 1;
        return new JxPageResult(data, page, Math.max(1, size), total, Math.max(0, pages));
    }

    public DBRowSet getData()       { return data; }
    public int      getPage()       { return page; }
    public int      getSize()       { return size; }
    public long     getTotal()      { return total; }
    public int      getTotalPages() { return totalPages; }
    public boolean  isHasNext()     { return page < totalPages - 1; }
    public boolean  isHasPrev()     { return page > 0; }
    public int      getOffset()     { return page * size; }
}
