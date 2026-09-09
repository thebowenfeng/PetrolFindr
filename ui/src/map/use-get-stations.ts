import type {BoundingBox, Coordinate, FuelType} from "../common/types.ts";
import {startTransition, useCallback, useRef, useState} from "react";
import type {GasStation} from "../api/types.ts";
import {filterGasStationsByLocation, searchGasStations} from "../api/client.ts";
import {calcLongLatDistance} from "../common/utils.ts";

const DISTANCE_BUCKETS = [1000, 2000, 4000, 8000, 16000];
const findLeastBucket = (distance: number) => {
    return DISTANCE_BUCKETS.filter((dist) => distance <= dist);
}

export const filterStationsByLocation = (coords: Coordinate, stations?: GasStation[], fuelType?: FuelType) => {
    const distanceBucketMap: Record<number, {
        cheapest: GasStation | undefined,
        cheapestDistance: number | undefined
    }> = DISTANCE_BUCKETS.reduce((prev, curr) => ({
        ...prev,
        [curr]: {
            cheapest: undefined,
            cheapestDistance: undefined
        }
    }), {});

    stations?.forEach((station) => {
        const distance = calcLongLatDistance(station.location, coords);
        if (distance === undefined) {
            return;
        }

        const buckets = findLeastBucket(distance);
        if (buckets) {
            buckets.map((bucket) => distanceBucketMap[bucket]).forEach((mapEntry) => {
                const prevFuelData = mapEntry.cheapest?.prices.find((price) => fuelType ? price.type === fuelType : price.type === 'U91');
                const currFuelData = station.prices.find((price) => fuelType ? price.type === fuelType : price.type === 'U91');
                if (currFuelData && (
                    prevFuelData === undefined ||
                    currFuelData.amount < prevFuelData.amount ||
                    (
                        currFuelData.amount === prevFuelData.amount &&
                        (
                            mapEntry.cheapestDistance === undefined ||
                            distance < mapEntry.cheapestDistance
                        )
                    )
                )) {
                    mapEntry.cheapest = station;
                    mapEntry.cheapestDistance = distance;
                }
            });
        }
    });

    const finalResults = Object.values(distanceBucketMap).map((val) => val.cheapest).filter((val) => val !== undefined);
    return [...new window.Map(finalResults.map((station) => [station.id, station])).values()];
}

const isEquivalentGasStations = (list1: GasStation[], list2: GasStation[]) => {
    if (list1.length != list2.length) {
        return false;
    }

    for (const station of list1) {
        if (!list2.some((val) => val.id === station.id)) {
            return false;
        }
    }
    return true;
}


export const useGetStations = () => {
    const [getStationRequest, setGetStationRequest] = useState<Promise<GasStation[] | undefined>>(Promise.resolve(undefined));
    const [getFilteredStationRequest, setFilteredGetStationRequest] = useState<Promise<GasStation[] | undefined>>(Promise.resolve(undefined));
    const getStationRequestRef = useRef(getStationRequest);

    const triggerGetStationRequest = useCallback((currentBox: BoundingBox, selfLocation?: Coordinate, fuelType?: FuelType, reloadStations: boolean = false) => {
        startTransition(() => {
            setFilteredGetStationRequest(Promise.resolve(undefined));
        })

        if (selfLocation && reloadStations) {
            const getByLocationUrl = new URL("http://localhost:8080/search/stations/location/stream");
            getByLocationUrl.searchParams.set("topRightLat", currentBox.topRight.latitude.toString());
            getByLocationUrl.searchParams.set("topRightLng", currentBox.topRight.longitude.toString());
            getByLocationUrl.searchParams.set("bottomLeftLat", currentBox.bottomLeft.latitude.toString());
            getByLocationUrl.searchParams.set("bottomLeftLng", currentBox.bottomLeft.longitude.toString());
            getByLocationUrl.searchParams.set("locationLat", selfLocation.latitude.toString());
            getByLocationUrl.searchParams.set("locationLng", selfLocation.longitude.toString());
            if (fuelType) {
                getByLocationUrl.searchParams.set("fuelType", fuelType);
            }

            const eventSource = new EventSource(getByLocationUrl);
            const getStationRequestPromise = Promise.withResolvers<GasStation[]>();
            const getFilteredStationRequestPromise = Promise.withResolvers<GasStation[]>();

            getStationRequestRef.current = getStationRequestPromise.promise;
            startTransition(() => {
                setGetStationRequest(getStationRequestPromise.promise);
            });

            eventSource.addEventListener('all-stations', (event) => {
                getStationRequestPromise.resolve(JSON.parse(event.data));
            });
            eventSource.addEventListener('filtered-stations', (event) => {
                const newFilteredStations: GasStation[] = JSON.parse(event.data);
                getFilteredStationRequestPromise.resolve(newFilteredStations);
                getStationRequestPromise.promise.then((stations) => {
                    if (stations === undefined || !isEquivalentGasStations(newFilteredStations, stations)) {
                        startTransition(() => {
                            setFilteredGetStationRequest(getFilteredStationRequestPromise.promise);
                        })
                    }
                })
                eventSource.close();
            });
            eventSource.onerror = () => {
                getStationRequestPromise.resolve([]);
                getFilteredStationRequestPromise.resolve([]);
                eventSource.close();
            }
        } else if (reloadStations) {
            const request = searchGasStations(currentBox);
            getStationRequestRef.current = request;
            startTransition(() => {
                setGetStationRequest(request);
            })
        } else if (selfLocation) {
            getStationRequestRef.current.then((stations) => {
                if (!stations) return;
                const filterGasStationPromise = filterGasStationsByLocation(selfLocation, stations, fuelType);
                filterGasStationPromise.then((filteredStations) => {
                    if (filteredStations && !isEquivalentGasStations(filteredStations, stations)) {
                        startTransition(() => {
                            setFilteredGetStationRequest(filterGasStationPromise);
                        })
                    }
                })
            })
        }
    }, []);

    return { getStationRequest, getFilteredStationRequest, triggerGetStationRequest };
}
