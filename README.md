<p align="center">
  <img src="src/main/resources/assets/overthought_vehicles/icon.png" width="160" alt="Overthought Vehicles">
</p>

<h1 align="center">Overthought Vehicles</h1>

<p align="center">
Vehicles thought through to the last bolt: cars, bikes, boats and planes, a fuel line from well to pump,<br>
and a garage where every part wears, fails and gets fixed.
</p>

<p align="center">
<b>Minecraft 1.21.1 · NeoForge 21.1.266+ · MIT</b><br>
<a href="https://github.com/Trossty/overthought-vehicles/wiki">Wiki</a> ·
<a href="https://github.com/Trossty/overthought-vehicles/issues">Issues</a>
</p>

## What's in it

- **19 vehicles**: sedans, a muscle car, an off-roader, pickup, tanker and tow trucks, a scooter and a sport bike, a quad, go-kart, dune buggy and electric golf cart, two boats, a light aircraft and a helicopter, plus a tractor and lawn tractor
- **Trailers and implements**: box and flatbed trailers, a caravan, tow dollies, a seed drill and a fertilizer spreader
- **Real driving**: speed-sensitive steering, handbrake drifts, an analog dashboard, engine sounds that follow the revs and gears
- **Fuel from the ground up**: oil deposits, a pumpjack, a refinery, a hydrotreater, pipes, tanks and a pump with a hose
- **A real workshop**: 24 parts per vehicle that wear and come off in order, a lift, an engine hoist, a service screen, an OBD tablet and diagnostic tools
- **Damage that matters**: crashes dent the side that took the hit, puncture tanks and hurt the people inside
- **Power and roads**: generators, cables, a battery bank, a charging post for electric vehicles, layered asphalt and road paint
- **Datapack vehicles**: a new vehicle is one JSON file
- Jade and WTHIT support, multiplayer, English and Turkish

## Controls

| Key | Action |
|-----|--------|
| W | Throttle |
| S | Brake / reverse |
| A / D | Steer |
| Space | Handbrake (drift at speed) |
| H | Horn |
| Shift | Get out |

The plane and helicopter fly with the same keys: see [Driving and Flying](https://github.com/Trossty/overthought-vehicles/wiki/Driving-and-Flying).

## Getting started

Build parts at the **Vehicle Workbench**, put the vehicle together at the **Vehicle Workstation**, open its crate with a **Wrench** and cut a **Car Key** for it. The [Getting Started](https://github.com/Trossty/overthought-vehicles/wiki/Getting-Started) page walks through it with the recipes.

## Building from source

```bash
./gradlew build                # the jar ends up in build/libs/
./gradlew runClient            # a development client
./gradlew runGameTestServer    # the game tests
```

Java 21 is needed. How the mod works inside is in the [developer notes](https://github.com/Trossty/overthought-vehicles/wiki/Developer-Notes) (Turkish); plans and postponed work are in [GELISTIRME.md](GELISTIRME.md).

## License

[MIT](LICENSE.txt): use it, change it, build on it and share it, as long as the licence notice goes with it.
