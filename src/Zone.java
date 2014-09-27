import java.io.Serializable;

class Point implements Serializable {
	double x;
	double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}

public class Zone implements Serializable {

	Point bottomPoint, topPoint;

	public String toString() {
		return bottomPoint.toString() + " to " + topPoint.toString();
	}

	Zone(Point bottomPoint, Point topPoint) {
		this.bottomPoint = bottomPoint;
		this.topPoint = topPoint;
	}
	
	double Area(){
		return (topPoint.y - bottomPoint.y) * (topPoint.x - bottomPoint.x);
	}
	
	boolean isEdgeCompleteOverlap(Zone zone){
		return completeHorizontalOverlap(zone) || completeVerticalOverlap(zone);
	}

	private boolean completeVerticalOverlap(Zone zone) {
		return (zone.topPoint.x == topPoint.x && zone.bottomPoint.x == bottomPoint.x);	
	}

	private boolean completeHorizontalOverlap(Zone zone) {
		return (zone.topPoint.y == topPoint.y && zone.bottomPoint.y == bottomPoint.y);		
	}

	boolean isPointInZone(Point point) {
		if ((point.x >= bottomPoint.x && point.x <= topPoint.x)
				&& (point.y >= bottomPoint.y && point.y <= topPoint.y))
			return true;
		else
			return false;
	}

	public Zone split() {
		if (isZoneSquare())
			return splitVertically();
		else
			return splitHorizontally();
	}

	/**
	 * Checks if zone is square shaped
	 */
	public boolean isZoneSquare() {
		double width = topPoint.x - bottomPoint.x;
		double height = topPoint.y - bottomPoint.y;

		return width == height;
	}
	
	public boolean isBroaderThanTaller() {
		double width = topPoint.x - bottomPoint.x;
		double height = topPoint.y - bottomPoint.y;
		
		return width > height;
	}

	/**
	 * Splits zone vertically and returns right half as new zone.
	 */
	public Zone splitVertically() {
		// Get width of new zone by splitting old one in half.
		double newWidth = (topPoint.x - bottomPoint.x) / 2;

		// Old zone set to new width.
		Zone newZone = new Zone(new Point(newWidth + bottomPoint.x,
				bottomPoint.y), new Point(topPoint.x, topPoint.y));

		this.topPoint.x = topPoint.x - newWidth;

		// New Zone with old zone split in half vertically.
		return newZone;

	}

	/**
	 * Splits zone horizontally and returns right half as new zone.
	 */
	public Zone splitHorizontally() {
		// Get height of new zone by splitting old one in half.
		double newHeight = (topPoint.y - bottomPoint.y) / 2;

		// Old zone set to new height.
		Zone newZone = new Zone(new Point(bottomPoint.x, newHeight
				+ bottomPoint.y), new Point(topPoint.x, topPoint.y));

		this.topPoint.y = this.topPoint.y - newHeight;

		// New Zone with old zone split in half horizontally.
		return newZone;

	}

	/**
	 * Return center cordinates of zone.
	 */
	public Point getCentre() {
		double centre_x = (this.topPoint.x + this.bottomPoint.x) / 2;
		double centre_y = (this.topPoint.y + this.bottomPoint.y) / 2;

		return new Point(centre_x, centre_y);
	}

	double getDistanceToPoint(Point destination) {
		Point center = getCentre();

		double x = (center.x - destination.x) * (center.x - destination.x);
		double y = (center.y - destination.y) * (center.y - destination.y);

		return Math.sqrt(x + y);
	}

	boolean isOverlap(Zone zone) {
		return (isVerticalOverlap(zone) || isHorizontalOverlap(zone));
	}
	
	// Check if this zone and zone in argument overlap horizontally.
	private boolean isHorizontalOverlap(Zone zone) {

		double mina, maxa, minb, maxb;

		if (bottomPoint.x == zone.bottomPoint.x)
			return true;

		if (bottomPoint.x < zone.bottomPoint.x) {
			mina = bottomPoint.x;
			maxa = topPoint.x;
			minb = zone.bottomPoint.x;
			maxb = zone.topPoint.x;
		} else {
			mina = zone.bottomPoint.x;
			maxa = zone.topPoint.x;
			minb = bottomPoint.x;
			maxb = topPoint.x;
		}

		if (minb >= maxa)
			return false;
		else if(minb > mina )
			return true;
		else return false;

	}
	
	// Check if this zone and zone in argument overlap vertically.
	private boolean isVerticalOverlap(Zone zone) {
		
		double mina, maxa, minb, maxb;

		if (bottomPoint.y == zone.bottomPoint.y)
			return true;

		if (bottomPoint.y < zone.bottomPoint.y) {
			mina = bottomPoint.y;
			maxa = topPoint.y;
			minb = zone.bottomPoint.y;
			maxb = zone.topPoint.y;
		} else {
			mina = zone.bottomPoint.y;
			maxa = zone.topPoint.y;
			minb = bottomPoint.y;
			maxb = topPoint.y;
		}

		if (minb >= maxa)
			return false;
		else if(minb > mina )
			return true;
		else return false;

	}
	
	// Merge this zone and zone in arguments.
	public void merge(Zone zone) {
		// Detect the direction of merge.
		if(bottomPoint.x == zone.bottomPoint.x)
			verticalMerge(zone);
		else 
			horizontalMerge(zone);			
	}

	private void horizontalMerge(Zone zone) {
		// Take over to right
		if(bottomPoint.x < zone.bottomPoint.x )
			topPoint.x = zone.topPoint.x;
		// Take over to left
		else
			bottomPoint.x = zone.bottomPoint.x;
	}

	private void verticalMerge(Zone zone) {
		
		// Take over upwards
		if(bottomPoint.y < zone.bottomPoint.y)
			topPoint.y = zone.topPoint.y;
		// Take over downwards.
		else
			bottomPoint.y = zone.bottomPoint.y;
		
	}

	public static void main(String[] args) {
		Zone z = new Zone(new Point(7.5, 0), new Point(10,5));
		Zone y = new Zone(new Point(5, 5), new Point(7.5, 10));

		System.out.println(y.isOverlap(z));

	}

	

	

}
