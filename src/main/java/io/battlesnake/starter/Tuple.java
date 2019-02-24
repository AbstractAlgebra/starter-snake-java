public static class Tuple
{
    int x;
    int y;

    public Tuple(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Tuple o)
    {
        return (this.x == o.x && this.y == o.y);
    }
}
