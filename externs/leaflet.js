"use strict";
/**
 * Leaflet map.
 *
 * @see http://leaflet.cloudmade.com/
 * @namespace
 * @externs
 */
var L = {};

/**
 * @constructor
 * @param {string} id
 * @param {Object} options
 */
L.Map = function(id, options){};

/**
 * @param {L.Layer} layer
 */
L.Map.prototype.addLayer = function(layer){};

/**
 * @param {L.Layer} layer
 */
L.Map.prototype.removeLayer = function(layer){};
L.Map.prototype.setView = function(a, b){};

L.Map.prototype.fitBounds = function(bounds){};

L.Map.prototype.invalidateSize = function(){};

/** @return {L.LatLng} */
L.Map.prototype.getCenter = function() {};

L.Map.prototype.getZoom = function() {};
L.Map.prototype.setZoom = function(z) {};

/** @constructor */
L.LatLng = function(a, b){};

/**
 * @constructor
 * @param {L.LatLng} latLng
 * @param {Object} options
 **/
L.marker = function(latLng, options){};
L.marker.prototype.bindPopup = function(a){};
L.marker.prototype.openPopup = function(){};
L.marker.prototype.addTo = function(a){};
L.marker.prototype.getLatLng = function(){};

/** @constructor */
L.TileLayer = function(a, b){};

/** @constructor */
L.TileLayer.WMS = function(a,b){};

/** @constructor */
L.Icon = function(a){};
L.Icon.prototype.iconSize = null;
L.Icon.prototype.shadowSize = null;
L.Icon.prototype.iconAnchor = null;
L.Icon.prototype.popupAnchor = null;
L.Icon.prototype.Default = {};
L.Icon.prototype.Default.imagePath = {};

/** @constructor */
L.Point = function(a, b){};

/** @constructor */
L.Layer = function () {};

/**
 * @constructor
 * @extends {L.Layer}
 */
L.StamenTileLayer = function (name) {};

L.Util = {};

/**
 * @param {Object} obj
 * @param {Object} options
 */
L.Util.setOptions = function (obj, options) {};

L.Map.prototype.scrollWheelZoom = {};
L.Map.prototype.scrollWheelZoom.disable = function () {};
L.Map.prototype.on = function () {};

/* Map event */
var e = {};
e.latlng = {};
e.latlng.lat = {};
e.latlng.lng = {};

L.divIcon = function(options){};

/**
 * @constructor
 * @struct
 * @param {number} x 
 * @param {number} y
 * @param {boolean=} opt_round
 */
L.Point = function(x,y,round) {};

var p1 = new L.Point(1,2);

/** {number} */
p1.x;

/** {number} */
p1.y;


    
/**
 * @constructor
 */
L.Path = function() {};


/**
 * @param {string} eventType
 * @param {function({{containerPoint: L.Point}})} callback
 */ 
L.Path.prototype.on = function(eventType, callback) {};
    
/**
 * @return {L.LatLng}
 */
L.Path.prototype.getBounds = function() {};

/**
 * @constructor
 * @extends {L.Path}
 */
L.Polyline = function(t,e) {};

/**
 * @constructor
 * @extends {L.Polyline}
 */
L.Polygon = function(t,e) {};

/**
 * @constructor
 * @extends {L.FeatureGroup}
 */
L.MultiPolygon = function(p,e) {};

/**
 * @param {string} eventType
 * @param {function({{containerPoint: L.Point}})} callback
 */ 
L.MultiPolygon.prototype.on = function(eventType, callback) {};

/**
 * @constructor
 * @extends {L.FeatureGroup}
 */
L.MultiPolyline = function(t,e) {};

/**
 * @constructor
 * @extends {L.Layer}
 */
L.Popup = function() {};

L.Popup.prototype.setContent = function(c) {};

/**
 * @param {L.LatLng} l
 */
L.Popup.prototype.setLatLng = function(l) {};
