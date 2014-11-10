(function() {
    var app = angular.module('app', []);
    
    app.controller('TaskController', ['$scope', '$http', function($scope, $http) {
        $scope.task = {
                dailyFlag: false
        };
        $scope.settingsOn = false;
        
        $http.get('/tasks.json').success(function(data) {
            $scope.tasks = data;
        });
        
        $scope.completeTask = function(index) {
            $http.put('/tasks/' + $scope.tasks[index].id + '/complete').success(function() {
                $scope.tasks.splice(index, 1)
            });
        };
        
        $scope.createTask = function() {
            $http.post('/tasks.json', $scope.task).success(function(data) {
                $scope.tasks.push(data);
                $scope.task = { dailyFlag: false };
            }).error(function() {
                console.log('Booo...');
            });
        }
        
        $scope.toggleDaily = function() {
            $scope.task.dailyFlag = !$scope.task.dailyFlag;
        }
        
        $scope.getTaskSteps = function(task) {
            return new Array(task.noSteps);
        }
        
        $scope.finishStep = function(index, stepNo) {
            var oldStepsValue = $scope.tasks[index].stepsCompleted;
            $scope.tasks[index].stepsCompleted = stepNo;
            $http.put('/tasks/' + $scope.tasks[index].id + '/step' + stepNo).success(function() {
                // play sound?
            }).error(function() {
                $scope.tasks[index].stepsCompleted = oldStepsValue;
            });
        }
        
        $scope.toggleSettings = function() {
            $scope.settingsOn = !$scope.settingsOn;
        }
    }]);
})();