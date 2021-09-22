fetch("http://ec2-18-219-118-171.us-east-2.compute.amazonaws.com:8085/leaderboard")
  .then(resp => resp.json())
  .then(leaderboard => {
    // thanks Arthur, https://stackoverflow.com/questions/14446511/most-efficient-method-to-groupby-on-an-array-of-objects
    const groupedMap = leaderboard.reduce(
        (entryMap, e) => entryMap.set(e.name, [...entryMap.get(e.name)||[], e]),
        new Map()
    );
    const colors = ["#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#b15928"]
    const playerDatasets = [];
    for (const [key, val] of groupedMap.entries()) {
        playerDatasets.push({label: key,
          fill: false,
          borderColor: colors[key.length % colors.length],
          backgroundColor: colors[key.length % colors.length] + "a0", // add some opacity
          cubicInterpolationMode: 'monotone',
          tension: 0.4,
          data: val.map(e => {
            let foo = {x: e.recordedAt, y: e.rank}
            return foo
            }),
        });
    }
    const config = {
      type: 'line',
      data: { datasets: playerDatasets },
      options: {
        scales: {
          y: {
            reverse: true,
            type: 'linear',
            title: {
              display: true,
              text: 'Rank'
            }
          },
          x: {
            type: 'timeseries',
            ticks: {
              source: 'data'
            },
            time: {
              unit: 'day'
            }
          }
        },
        plugins: {
          title: {
            display: true,
            text: 'Dota 2 Leaderboard Tracker (NA)'
          },
          legend: {
            position: 'bottom'
          }
        }
      }
    };
    let myChart = new Chart(document.getElementById('myChart'), config);
  });

