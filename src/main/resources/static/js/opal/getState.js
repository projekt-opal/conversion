function getState(portalName) {

    apiKey = $('#ckanApiKey').val();
    alert('#' + portalName + '_lnf');
    lnf = $('#' + portalName + '_lnf').val();
    high = $('#' + portalName + '_high').val();

    $(document).ready(function(){
        $('' +
            '<form action="/convert">' +
                '<input name="portalName" value="' + portalName + '">' +
                '<input name="lnf" value="' + lnf + '">' +
                '<input name="high" value="' + high + '">' +
                '<input name="apiKey" value="' + apiKey + '">' +
            '</form>').appendTo('body').submit();
    });
}