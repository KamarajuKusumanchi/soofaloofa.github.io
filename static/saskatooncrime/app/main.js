define(function(require) {
  var d3 = require('d3');
  var d3tip = require('d3tip');
  var queue = require('queue');
  var colorbrewer = require('colorbrewer');

  var width = 800;
  var height = 800;
  var startingYear = 2012;

  // Create SVG element
  var svg = d3.select("#map")
        .append("svg")
        .attr("width", width)
        .attr("height", height);

  // Create an initial projection and path
  var projection = d3.geo.albers().scale(1).translate([0, 0]);
  var path = d3.geo.path().projection(projection);

  var maxCrimes = {}; 
  var neighbourhoods;

  queue()
    .defer(d3.json, 'data/saskatoon-geo.json')
    .defer(d3.csv, 'data/crimes-year-neighbourhood.csv')
    .await(visualize);

  function visualize(error, geojson, data) {
    // Calculate the max crimes for each year
    for (var i = 0; i < data.length; i++ ) {
      var dataYear = parseInt(data[i].Year.split("-")[0]);
      maxCrimes[dataYear] = Math.max(maxCrimes[dataYear] || 0, data[i].Crimes);
    }

    // Merge crime data with GeoJSON data
    // Value of the GeoJSON property will hold an array of Year/Crimes,
    for (var i = 0; i < geojson.features.length; i++) {
      geojson.features[i].properties.Value = [];
      for (var j = 0; j < data.length; j++) {
        var dataNeighbourhood = data[j].Neighbourhood;
        var dataCrimes = parseInt(data[j].Crimes);
        var dataYear = parseInt(data[j].Year.split("-")[0]);

        if (dataNeighbourhood === geojson.features[i].properties.Name) {
          geojson.features[i].properties.Value.push({
            crimes: dataCrimes,
            year: dataYear
          });
        }
      }
    }

    // Center the map:
    // http://stackoverflow.com/questions/14492284/center-a-map-in-d3-given-a-geojson-object
    var b = path.bounds(geojson);  // Compute the bounds of the feature
    var s = 0.95 / Math.max((b[1][0] - b[0][0]) / width, (b[1][1] - b[0][1]) / height);  // Compute scale by comparing aspect ratio
    var t = [(width - s * (b[1][0] + b[0][0])) / 2, (height - s * (b[1][1] + b[0][1])) / 2];  // Translate to center of bouding box

    // Update projection using correct scale and translation
    projection.scale(s).translate(t);

    var quantize = calculateQuantizeScale(startingYear);
    
    // Create the initial paths
    var neighbourhoods = svg.append("g").attr("id", "neighbourhood");

    // Display the initial values
    neighbourhoods.selectAll("path")
        .data(geojson.features)
        .enter()
        .append("path")
        .attr("d", path)
        .style("fill", function(d) {
          return calculateFill(d, startingYear);
        })
        .style("fill-opacity", .7)
        .style("stroke", '#aaa');


    // Bind an event listener for the current year
    d3.select("#year-slider").on("input", function() {
      update(+this.value);
    });


    function calculateQuantizeScale(year) {
      return d3.scale.quantize().domain([0, maxCrimes[year]]).range(colorbrewer.Blues[9]);
    }

    function currentCrimes(d, year) {
      var crimesYear = d.properties.Value.filter(function(x) { return x.year == year; } );
      var numCrimes = crimesYear.map(function(x) { return x.crimes; } );
      return d3.sum(numCrimes);
    }

    function calculateFill(d, year) {
      return quantize(currentCrimes(d, year));
    }

    var tip = d3tip()
      .attr('class', 'd3-tip')
      .html(function(d, year) { 
        return '<span>' + d.properties.Name + ':</span>' + '<br />' + '<span>' + currentCrimes(d, year) + ' Crimes </span>'
      })
      .offset([-12, 0])

    update(startingYear);

    function update(year) {
      var quantize = calculateQuantizeScale(year);

      d3.selectAll("#year-value")
        .text(year);

      neighbourhoods.call(tip);

      neighbourhoods.selectAll("path")
        .transition()
        .duration(500)
        .style("fill", function(d) {
          return calculateFill(d, year);
        });

      neighbourhoods.selectAll("path")
        .on("mouseover", function(d) {
          d3.select(this).style("fill-opacity", 1);
          tip.show(d, year);
        })
        .on("mouseout", function(d) {
          d3.select(this).style("fill-opacity", 0.7);
          tip.hide(d, year);
        });
    }
  }
});
