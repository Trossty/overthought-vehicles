package me.trossty.car.client.render;

/**
 * What a rider's drawing needs to know about the vehicle under them, carried
 * on their render state: how far the bike leans and about what, and where
 * their hands go.
 */
public interface RiderPose {

    /** Not at the controls: drawn the vanilla way. */
    int GRIP_NONE = 0;
    /** Hands on a steering wheel. */
    int GRIP_WHEEL = 1;
    /** Hands on handlebars, knees in. */
    int GRIP_BARS = 2;

    void car$setPose(int grip, float steer, float lean, float leanPivot, float vehicleYaw);

    int car$grip();

    float car$steer();

    /** Degrees of lean about the vehicle's own forward axis. */
    float car$lean();

    /** Height of the lean's pivot above the rider's feet, in blocks. */
    float car$leanPivot();

    float car$vehicleYaw();
}
