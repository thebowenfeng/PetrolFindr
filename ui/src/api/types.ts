import type {Coordinate, FuelType} from "../common/types.ts";

interface TradingHour {
    startMinute: number,
    endMinute: number
}

interface Price {
    type: FuelType,
    updated: number,
    amount: number
}

export interface GasStation {
    id: string,
    name: string,
    location: Coordinate,
    address: string,
    tradingHours: TradingHour[] | undefined,
    icon: string,
    prices: Price[]
}

export interface PhotonFeatureProperties {
    osm_type: 'N' | 'W' | 'R',
    osm_id: number,
    osm_key: string,
    osm_value: string,
    type?: string,
    name?: string,
    housenumber?: string,
    street?: string,
    postcode?: string,
    locality?: string,
    district?: string,
    city?: string,
    county?: string,
    state?: string,
    country?: string,
    countrycode?: string,
    extent?: [west: number, north: number, east: number, south: number],
    extra?: Record<string, unknown>
}

export interface PhotonFeature {
    type: 'Feature',
    geometry: {
        type: 'Point',
        coordinates: [longitude: number, latitude: number]
    },
    properties: PhotonFeatureProperties
}

export interface PhotonResponse {
    type: 'FeatureCollection',
    features: PhotonFeature[]
}
