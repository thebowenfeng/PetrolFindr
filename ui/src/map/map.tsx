import {
    startTransition,
    Suspense,
    use, useCallback,
    useEffect,
    useMemo,
    useRef,
    useState
} from 'react';
import {Map, MapMouseEvent, setWorkerUrl} from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url';
import {StationMarker} from "./marker.tsx";
import {debounce} from "../common/utils.ts";
import type {GasStation} from "../api/types.ts";
import type {Coordinate, FuelType} from "../common/types.ts";
import {LocationMarker, SelfLocationMarker} from "./current-location-marker.tsx";
import {filterStationsByLocation, useGetStations} from "./use-get-stations.ts";

setWorkerUrl(workerUrl);

interface MarkerListProps {
    getStationRequest: Promise<GasStation[] | undefined>,
    getFilteredStationRequest: Promise<GasStation[] | undefined>,
    map: Map,
    mapFilter?: MapFilter
}

interface MapFilter {
    gasTypeFilter?: FuelType
    gpsLocationFilter?: Coordinate
    customLocationFilter?: { enabled: boolean, coordinate?: Coordinate }
}

interface MapComponentProps {
    mapFilter?: MapFilter;
    onMapClick?: (coordinate: Coordinate) => void;
}

const MarkerList = ({ getStationRequest, getFilteredStationRequest, map, mapFilter }: MarkerListProps) => {
    const result = use(getStationRequest);
    const filteredResult = use(getFilteredStationRequest);

    const gpsFiltered = useMemo(() => {
        if (!mapFilter?.gpsLocationFilter && (!mapFilter?.customLocationFilter?.enabled || !mapFilter?.customLocationFilter?.coordinate)) {
            return result;
        }
        const coords = mapFilter?.customLocationFilter?.enabled && mapFilter?.customLocationFilter?.coordinate
            ? mapFilter.customLocationFilter.coordinate! : mapFilter.gpsLocationFilter!;

        return filterStationsByLocation(coords, result, mapFilter.gasTypeFilter);
    }, [result, mapFilter])

    const filterGasStation = (gasStation: GasStation) => {
        if (mapFilter) {
            if (mapFilter.gasTypeFilter) {
                return gasStation.prices.some((price) => price.type === mapFilter.gasTypeFilter);
            }
        }
        return true;
    }

    if (filteredResult && !filteredResult.some((station) => !station.prices.some((price) => price.type === mapFilter?.gasTypeFilter))) {
        return filteredResult.map((station) => {
            const stationCopy = {...station};
            if (mapFilter) {
                if (mapFilter.gasTypeFilter) {
                    stationCopy.prices = station.prices.filter((price) => price.type === mapFilter.gasTypeFilter);
                }
            }

            return (
                <StationMarker
                    station={stationCopy}
                    map={map}
                    key={station.id}
                    showPrice={mapFilter?.gpsLocationFilter !== undefined || mapFilter?.gasTypeFilter !== undefined || mapFilter?.customLocationFilter?.coordinate !== undefined}
                />
            )
        })
    }

    return map && gpsFiltered?.filter(filterGasStation).map((station) => {
        const stationCopy = {...station};
        if (mapFilter) {
            if (mapFilter.gasTypeFilter) {
                stationCopy.prices = station.prices.filter((price) => price.type === mapFilter.gasTypeFilter);
            }
        }

        return (
            <StationMarker
                station={stationCopy}
                map={map}
                key={station.id}
                showPrice={mapFilter?.gpsLocationFilter !== undefined || mapFilter?.gasTypeFilter !== undefined || mapFilter?.customLocationFilter?.coordinate !== undefined}
            />
        );
    })
};

export const MapComponent = ({ mapFilter, onMapClick }: MapComponentProps) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const [map, setMap] = useState<Map | undefined>(undefined);
    const { getStationRequest, getFilteredStationRequest, triggerGetStationRequest } = useGetStations();
    const locationCoordFilter = mapFilter?.customLocationFilter?.enabled ? mapFilter.customLocationFilter.coordinate : mapFilter?.gpsLocationFilter;

    const setBounds = useCallback(() => {
        if (!map) return;

        const bounds = map.getBounds();
        const topRight = bounds.getNorthEast();
        const bottomLeft = bounds.getSouthWest();

        startTransition(() => {
            triggerGetStationRequest({
                topRight: {
                    longitude: topRight.lng,
                    latitude: topRight.lat
                },
                bottomLeft: {
                    longitude: bottomLeft.lng,
                    latitude: bottomLeft.lat
                },
            }, locationCoordFilter, mapFilter?.gasTypeFilter, true);
        })
    }, [locationCoordFilter, map, mapFilter?.gasTypeFilter, triggerGetStationRequest]);

    const handleMapClick = useCallback((ev: MapMouseEvent) => {
        onMapClick?.({ longitude: ev.lngLat.lng, latitude: ev.lngLat.lat });
    }, [onMapClick]);

    useEffect(() => {
        if (!map || !locationCoordFilter) {
            return;
        }

        const bounds = map.getBounds();
        const topRight = bounds.getNorthEast();
        const bottomLeft = bounds.getSouthWest();

        triggerGetStationRequest({
            topRight: {
                longitude: topRight.lng,
                latitude: topRight.lat
            },
            bottomLeft: {
                longitude: bottomLeft.lng,
                latitude: bottomLeft.lat
            }
        }, locationCoordFilter, mapFilter?.gasTypeFilter, false);
    }, [locationCoordFilter, map, mapFilter?.gasTypeFilter, triggerGetStationRequest]);

    useEffect(() => {
        const handleMove = debounce(300, setBounds);
        let localMap: Map;
        const initMap = (initialCoords: Coordinate | undefined) => {
            if (!containerRef.current || map !== undefined) {
                return;
            }

            localMap = new Map({
                container: containerRef.current,
                style: 'https://tiles.openfreemap.org/styles/bright',
                center: initialCoords ? [initialCoords.longitude, initialCoords.latitude] : [144.9631, -37.8136],
                zoom: 14,
            });
            setMap(localMap);

            localMap.on('move', handleMove);
            localMap.on('click', handleMapClick);

            const bounds = localMap.getBounds();
            const topRight = bounds.getNorthEast();
            const bottomLeft = bounds.getSouthWest();

            triggerGetStationRequest({
                topRight: {
                    longitude: topRight.lng,
                    latitude: topRight.lat
                },
                bottomLeft: {
                    longitude: bottomLeft.lng,
                    latitude: bottomLeft.lat
                }
            }, undefined, undefined, true);
        }

        if ('geolocation' in navigator) {
            navigator.geolocation.getCurrentPosition((pos) => {
                initMap({ longitude: pos.coords.longitude, latitude: pos.coords.latitude });
            }, () => {
                initMap(undefined);
            })
        } else {
            initMap(undefined);
        }

        if (map) {
            map.on('move', handleMove);
            map.on('click', handleMapClick);
        }

        return () => {
            (map ?? localMap)?.off('move', handleMove);
            (map ?? localMap)?.off('click', handleMapClick);
        }
    }, [handleMapClick, map, setBounds, triggerGetStationRequest]);

    return (
        <>
            <div ref={containerRef} className="map" />
            <Suspense>
                {map && (
                    <MarkerList
                        getStationRequest={getStationRequest}
                        getFilteredStationRequest={getFilteredStationRequest}
                        map={map}
                        mapFilter={mapFilter}
                    />
                )}
            </Suspense>
            {map && mapFilter?.customLocationFilter === undefined && <SelfLocationMarker map={map} />}
            {map && mapFilter?.customLocationFilter?.coordinate && mapFilter.customLocationFilter?.enabled && <LocationMarker map={map} coordinate={mapFilter?.customLocationFilter?.coordinate} />}
        </>
    );
};
