package com.bowenfeng.petrolfindr.navigation.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NavigationResponse(
    val code: String,
    val routes: List<Route> = emptyList(),
    val waypoints: List<Waypoint> = emptyList(),
    val message: String? = null,
    @JsonProperty("data_version")
    val dataVersion: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Route(
    val legs: List<RouteLeg>,
    @JsonProperty("weight_name")
    val weightName: String,
    val geometry: String,
    val weight: Double,
    val duration: Double,
    val distance: Double,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RouteLeg(
    val steps: List<RouteStep> = emptyList(),
    val weight: Double,
    val summary: String,
    val duration: Double,
    val distance: Double,
    val annotation: Annotation? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RouteStep(
    val distance: Double,
    val duration: Double,
    val geometry: String,
    val weight: Double,
    val name: String,
    val mode: String,
    val maneuver: StepManeuver,
    val intersections: List<Intersection>,
    val ref: String? = null,
    val pronunciation: String? = null,
    val destinations: String? = null,
    val exits: String? = null,
    @JsonProperty("rotary_name")
    val rotaryName: String? = null,
    @JsonProperty("rotary_pronunciation")
    val rotaryPronunciation: String? = null,
    @JsonProperty("driving_side")
    val drivingSide: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StepManeuver(
    val location: List<Double>,
    @JsonProperty("bearing_before")
    val bearingBefore: Int,
    @JsonProperty("bearing_after")
    val bearingAfter: Int,
    val type: String,
    val modifier: String? = null,
    val exit: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Intersection(
    val location: List<Double>,
    val bearings: List<Int>,
    val entry: List<Boolean>,
    val classes: List<String> = emptyList(),
    val `in`: Int? = null,
    val out: Int? = null,
    val lanes: List<Lane> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Lane(
    val indications: List<String>,
    val valid: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Annotation(
    val distance: List<Double>? = null,
    val duration: List<Double>? = null,
    val datasources: List<Int>? = null,
    val nodes: List<Long>? = null,
    val weight: List<Double>? = null,
    val speed: List<Double>? = null,
    val metadata: AnnotationMetadata? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnnotationMetadata(
    @JsonProperty("datasource_names")
    val datasourceNames: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Waypoint(
    val hint: String? = null,
    val location: List<Double>,
    val name: String,
    val distance: Double,
)
