(function() {
    var app = angular.module('app', []);
    
    app.controller('TaskController', ['$scope', '$http', function($scope, $http) {
        $http.get('/tasks.json').success(function(data) {
            $scope.tasks = data;
        });
        
        $scope.completeTask = function(index) {
            $http.put('/tasks/' + $scope.tasks[index].id).success(function() {
                $scope.tasks.splice(index, 1)
            });
        };
    }]);
})();