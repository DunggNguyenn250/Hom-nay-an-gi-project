package org.example.homnayangi.helper;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Tính khoảng cách giữa hai tọa độ địa lý (Latitude, Longitude) theo công thức Haversine.
     *
     * @param lat1 Vĩ độ điểm 1
     * @param lon1 Kinh độ điểm 1
     * @param lat2 Vĩ độ điểm 2
     * @param lon2 Kinh độ điểm 2
     * @return Khoảng cách tính bằng Kilomet (km)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
