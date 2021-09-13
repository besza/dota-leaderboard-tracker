fetch("http://localhost:8080/leaderboard")
  .then(resp => resp.json())
  .then(leaderboard => {
    const data = {
      labels: [...Array(leaderboard.length).keys()],
      datasets: [{
        label: 'Masao',
        backgroundColor: 'rgb(255, 0, 0)',
        borderColor: 'rgb(255, 0, 0)',
        data: leaderboard.filter(r => r.name === 'mason').map(e => e.rank),
      },
      {
        label: 'Arteezy',
        backgroundColor: 'rgb(0, 255, 0)',
        borderColor: 'rgb(0, 255, 0)',
        data: leaderboard.filter(r => r.name === 'Arteezy').map(e => e.rank),
      },
      {
       label: 'Gunnar',
       backgroundColor: 'rgb(0, 0, 255)',
       borderColor: 'rgb(0, 0, 255)',
       data: leaderboard.filter(r => r.name === 'Gunnar').map(e => e.rank),
       }]
    };
    const config = {
      type: 'line',
      data: data,
      options: {
        scales: {
          y: {
            reverse: true,
            type: 'linear',
            min: 1
          },
          x: {
            reverse: true
          }
        }
      }
    };
    var myChart = new Chart(document.getElementById('myChart'), config);
  });

