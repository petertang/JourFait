(function() {
    var app = angular.module('app', []);
    
    app.controller('TaskController', ['$scope', '$http', function($scope, $http) {
        $scope.task = {
                dailyFlag: false
        };
        $http.get('/tasks.json').success(function(data) {
            $scope.tasks = data;
        });
        
        $scope.completeTask = function(index) {
            $http.put('/tasks/' + $scope.tasks[index].id).success(function() {
                $scope.tasks.splice(index, 1)
            });
        };
        
        $scope.createTask = function() {
            $http.post('/tasks.json', $scope.task).success(function() {
                $scope.tasks.push($scope.task);
                $scope.task = { dailyFlag: false };
            }).error(function() {
                console.log('Booo...');
            });
        }
    }]);
})();