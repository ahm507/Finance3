(function() {
    'use strict';

    angular
        .module('financeApp')
        .controller('UserSettingsDetailController', UserSettingsDetailController);

    UserSettingsDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'UserSettings'];

    function UserSettingsDetailController($scope, $rootScope, $stateParams, previousState, entity, UserSettings) {
        var vm = this;

        vm.userSettings = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('financeApp:userSettingsUpdate', function(event, result) {
            vm.userSettings = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
