<!-- ProxyDatumBias -->
<div class="tab-pane container-fluid" id="bias">
	<div class="row-fluid">
		<div class="span4"><h3>Proxy-Datum Bias</h3></div>
		<div class="span8" id="bias-alert-container"></div>
	</div>
	<ul class="nav nav-tabs" id="action-bias-tablist">
		<li class="active"><a  data-toggle="tab" href="#bias-view-tab">View</a></li>
		<li><a data-toggle="tab" href="#bias-manage-tab">Manage</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="bias-view-tab">
			<select id="bias-list" class="feature-list"></select>
		</div>
		<div class="tab-pane" id="bias-manage-tab">
			<div id="bias-uploader" class="uploader"></div>
			<button class="btn btn-success" id="bias-triggerbutton"><i class="icon-arrow-up icon-white"></i>Upload</button>
			<button id="bias-remove-btn" disabled class="btn btn-success">
				<i class="icon-remove icon-white"></i>
				&nbsp;Remove
			</button>
		</div>
	</div>
</div> <!-- /ProxyDatumBias -->