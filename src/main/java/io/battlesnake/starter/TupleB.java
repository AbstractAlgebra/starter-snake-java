public class TupleB
{
    public int x;
    public int y;

    public TupleB(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public boolean equals(TupleB o)
    {
        return (this.x == o.x && this.y == o.y);
    }
}
