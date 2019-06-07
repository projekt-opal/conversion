function startFetching(portalName) {

    apiKey = $('#ckanApiKey').val();
    lnf = $('#' + portalName + '_lnf').val();
    high = $('#' + portalName + '_high').val();

    alert('Starting fetching ' + portalName);

    $(document).ready(function(){
        $('' +
            '<form style="display: none" action="/convert">' +
                '<input name="portalName" value="' + portalName + '">' +
                '<input name="lnf" value="' + lnf + '">' +
                '<input name="high" value="' + high + '">' +
                '<input name="apiKey" value="' + apiKey + '">' +
            '</form>').appendTo('body').submit();
    });

}