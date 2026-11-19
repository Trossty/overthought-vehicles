// The Hilux as a recovery truck: the same pickup with a boom over the
// tailgate, a winch behind the cab, lockers along the bed and a bar of
// amber beacons on the roof.
k.include('pickup_truck');

const BOOM = '#3B3B40';

k.g('body');
// lockers along the bed's sides, chequer plate with a handle each
k.sym(() => {
    k.box(11.6, 13.04, -40.0, 14.2, 17.0, -26.0, C.CHROME);
    k.box(11.5, 13.04, -33.1, 11.6, 17.0, -32.9, C.DARK_STEEL);
    k.box(11.4, 15.6, -37.6, 11.6, 16.0, -35.6, C.BLACK);
    k.box(11.4, 15.6, -30.4, 11.6, 16.0, -28.4, C.BLACK);
});
// the winch: a drum of cable between two cheeks, on a bed plate
k.box(-7.0, 13.04, -46.0, 7.0, 13.6, -42.0, C.DARK_STEEL);
k.box(-5.6, 14.2, -45.4, 5.6, 17.4, -42.6, C.BLACK);
k.box(-5.2, 14.5, -45.55, 5.2, 17.1, -42.45, '#55585E');
k.sym(() => k.box(5.6, 13.6, -45.8, 6.4, 18.0, -42.2, C.CHROME));
k.box(-8.6, 13.6, -45.2, -6.4, 16.4, -42.8, '#2F3136');
// the frame: a post each side, a bar across, a brace forward from each post
k.sym(() => {
    k.box(8.6, 13.04, -46.0, 11.0, 29.0, -43.6, BOOM);
    k.beam([9.8, 13.6, -30.0], [9.8, 23.0, -43.8], 1.5, 1.4, BOOM);
});
k.box(-11.0, 29.0, -46.4, 11.0, 31.0, -43.2, BOOM);
// the boom, the sheave at its head, the cable and the hook
k.beam([0, 29.6, -44.4], [0, 26.6, -57.6], 3.4, 2.8, 'paint_dark');
k.sym(() => k.beam([8.4, 29.4, -45.0], [1.6, 27.6, -55.0], 1.0, 1.0, BOOM));
k.box(-1.2, 24.8, -59.0, 1.2, 27.4, -56.4, C.CHROME);
k.box(-1.5, 25.6, -58.2, 1.5, 26.6, -57.2, C.DARK_STEEL);
k.box(-0.25, 18.0, -58.0, 0.25, 25.0, -57.5, C.BLACK);
k.box(-0.7, 16.9, -58.3, 0.7, 18.0, -57.2, C.STEEL);
k.box(-1.0, 15.6, -58.6, 1.0, 16.9, -56.9, '#B5221E');
k.beam([0, 17.2, -45.2], [0, 26.2, -56.8], 0.4, 0.4, C.BLACK);

// the beacons: a bar across the roof, an amber lens at each end
k.box(-9.0, 32.5, 0.0, 9.0, 33.5, 3.0, C.BLACK);
k.sym(() => {
    k.box(3.0, 33.5, 0.4, 8.5, 34.9, 2.6, '#E0F0A020');
    k.box(5.2, 33.5, 1.1, 6.3, 34.5, 1.9, '#F2B13A');
});
k.box(-2.4, 33.5, 0.6, 2.4, 34.3, 2.4, C.DARK_STEEL);
