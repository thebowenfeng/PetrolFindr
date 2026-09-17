import { Geodesic } from 'geographiclib-geodesic';
import type {Coordinate} from "./types.ts";

export const debounce = <Args extends unknown[]>(timeout: number, func: (...args: Args) => void) => {
    let timeoutId: ReturnType<typeof setTimeout> | undefined;

    return (...args: Args) => {
        if (timeoutId !== undefined) {
            clearTimeout(timeoutId);
        }
        timeoutId = setTimeout(func, timeout, ...args);
    }
}

export const calcLongLatDistance = (coord1: Coordinate, coord2: Coordinate) => {
    return Geodesic.WGS84.Inverse(
        coord1.latitude,
        coord1.longitude,
        coord2.latitude,
        coord2.longitude
    ).s12;
}
