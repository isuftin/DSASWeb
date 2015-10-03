<div id="application-overlay" style="height:100%;width:100%;position:fixed;top:0;left:0;background-color:#FFFFFF;z-index:9999;">
    <div id="application-overlay-content" style='height: 50%;padding-left: 25%;position: relative;top: 15%;width: 50%;color: #333333;font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;font-size: 14px;line-height: 20px;'>
        <div style="text-align: center"><h1 style="letter-spacing: 0.5em;font-size: 38.5px; line-height: 40px;color: inherit; font-family: inherit; font-weight: bold;text-rendering: optimizelegibility;">Coastal Hazards</h1></div>
        <div style="width: 100%;max-width: none;border: 0 none;height: auto;vertical-align: middle;">
            <img id="application-overlay-banner" src="images/splash/splash.png" style="height:45%;width:75%" />
        </div>
        This web-based Digital Shoreline Analysis System (DSASweb) is a software application that enables a user to calculate shoreline rate-of-change statistics from multiple historical shoreline positions.
        <br /><br />
        A user-friendly interface of simple buttons and menus guides the user through the major steps of shoreline change analysis.
        <br /><br />
        You can use our current database of shorelines, or upload your own.
        <br /><br />
        DSASweb is a convenient, web-based version of the original USGS DSAS analysis tool.
        <br /><br />
        <div style="text-align:center;">
            <div id="splash-status-update"></div>
            <img id="splash-spinner" alt="spinner image" src="images/spinner/spinner3.gif" />
        </div>
    </div>
</div>
<script type="text/javascript">
    var splashUpdate = function(message) {
        $('#splash-status-update').html(message);
    };
    splashUpdate("Loading application...");
</script>