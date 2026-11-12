package me.trossty.car.fluid;

/**
 * Something the station's own plumbing draws petrol from when it is bolted
 * on: a pipe's open end, a yard tank, a valve or a pump standing against it.
 * The hydrotreater is the one that makes it; nothing upstream of it has any
 * petrol a car could use.
 */
public interface PetrolSource {

    /** Gives up to {@code amount} units of petrol and says how many it gave. */
    int drainPetrol(int amount);
}
