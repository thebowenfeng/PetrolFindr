import type {BoundingBox, Coordinate, FuelType} from "../common/types.ts";
import type {GasStation} from "./types.ts";

let cache: GasStation[] = [];

export const searchGasStations = async (currentBox: BoundingBox) => {
    const response = await fetch("http://localhost:8080/search/stations", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(currentBox)
    });
    if (!response.ok) {
        return cache;
    }
    const result = await response.json() as GasStation[];
    cache = result;
    return result;
}

export const filterGasStationsByLocation = async (location: Coordinate, allStations: GasStation[], fuelType?: FuelType) => {
    const response = await fetch("http://localhost:8080/search/stations/location", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({allStations, location, fuelType})
    });
    if (!response.ok) {
        return undefined;
    }
    return await response.json() as GasStation[];
}