(function() {
    var app = angular.module('app', []);
    
    app.controller('TaskController', ['$scope', '$http', function($scope, $http) {
        $scope.task = {
                dailyFlag: false,
        };
        $scope.settingsOn = false;
        
        $http.get('/tasks.json').success(function(data) {
            $scope.tasks = data;
        });
        
        $scope.completeTask = function(index) {
            var taskToDelete = $scope.tasks[index];
            $scope.tasks.splice(index, 1)
            $http.put('/tasks/' + taskToDelete.id + '/complete').success(function() {
              // play sound?  
            }).error(function() {
                // TODO: report error and put back task
                $scope.tasks.push(taskToDelete);
            });
        };
        
        $scope.createTask = function() {
            $http.post('/tasks.json', $scope.task).success(function(data) {
                $scope.tasks.push(data);
                $scope.task = { dailyFlag: false };
            }).error(function(data) {
                console.log(data);
            });
        }
        
        $scope.toggleDaily = function() {
            $scope.task.dailyFlag = !$scope.task.dailyFlag;
        }
        
        $scope.getArrayOfSize = function(size) {
            var array = new Array();
            for (i = 0; i<size; i++) {
                array.push(i+1);
            }
            return array;
        }
        
        $scope.finishStep = function(index, stepNo) {
            var oldStepsValue = $scope.tasks[index].stepsCompleted;
            $scope.tasks[index].stepsCompleted = stepNo;
            $http.put('/tasks/' + $scope.tasks[index].id + '/step' + stepNo).success(function() {
                // play sound?
            }).error(function() {
                // TODO: report error
                $scope.tasks[index].stepsCompleted = oldStepsValue;
            });
        }
        
        $scope.toggleSettings = function() {
            $scope.settingsOn = !$scope.settingsOn;
            if ($scope.settingsOn) {
                //$scope.task.startDate = new Date();
            } else {
                delete $scope.task.startDate;
            }
        }
    }]);
    
    app.controller('UserController', ['$scope', '$http', '$window', function($scope, $http, $window) {
        $scope.user = {}
        $scope.register = function() {
            $http.post('/accounts', $scope.user).success(function(data) {
                console.log('success');
                $scope.user = {};
                $window.location.href="/tasks";
            }).error(function(data) {
                console.log('error ');
                console.log(data);
                // redisplay register page
            });
        }
        
        $scope.login = function() {
            $http.post('/login', $scope.user).success(function(data) {
               console.log('Login successful'); 
               $window.location.href="/tasks";
            }).error(function(data) {
               console.log('Error logging in');
            });
        }
    }])
})();