import java.util.*;

public final class RSPos
{
	public final int x, y, z;

	public RSPos(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public int hashCode()
	{
		int hash = 1;

		hash = hash * 31 + this.y;
		hash = hash * 53 + this.x;
		hash = hash * 19 + this.z;

		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof RSPos)
		{
			RSPos op = (RSPos)o;

			return true
				&& op.x == this.x
				&& op.y == this.y
				&& op.z == this.z;

		}

		return false;
	}

	@Override
	public String toString()
	{
		return String.format("RSPos(%d, %d, %d)", x, y, z);
	}
}

