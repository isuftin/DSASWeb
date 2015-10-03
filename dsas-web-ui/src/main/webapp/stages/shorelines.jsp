<!-- Shorelines -->
<div class="tab-pane container-fluid active" id="shorelines">
	<div class="row-fluid">
		<div class="span4"><h3>Shorelines</h3></div>
		<div class="span8" id="shorelines-alert-container"></div>
	</div>
	<ul class="nav nav-tabs" id="action-shorelines-tablist">
		<li class="active"><a  data-toggle="tab" href="#shorelines-view-tab">View</a></li>
		<li><a data-toggle="tab" href="#shorelines-manage-tab">Manage</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="shorelines-view-tab">
			<p class="text-center">
				<button data-toggle="button" class="btn btn-success" id="shorelines-aoi-select-toggle">
					<i class="fa fa-map-marker icon-white"></i> Select Area Of Interest
				</button>
			</p>
			<div id="description-aoi" class="well hidden">
				<h3>Selecting An Area Of Interest</h3>
				<p>
					Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin 
					pretium varius ipsum, sit amet sollicitudin mauris tristique non. 
					Sed ligula leo, ullamcorper non felis sit amet, facilisis luctus dui. 
					Aliquam vel magna nunc. Mauris quam arcu, viverra id lacus sodales, 
					imperdiet cursus magna. In vel efficitur lacus.
				</p>
				<p class="text-center">
					<button class="btn btn-success" id="shorelines-aoi-select-done">
						<i class="fa fa-check icon-white"></i> I'm Done
					</button>
				</p>
			</div>
			<div id="shorelines-feature-table-container" class="hidden">
				<table class="table table-bordered table-condensed tablesorter shoreline-table">
					<thead>
					<th class="shoreline-table-selected-head-column" data-column="0">Visibility</th>
					<th data-column="1">Date</th>
					<th data-column="2">Source</th>
					<th data-sorter="false">Color</th>
					</thead>
					<tbody>

					</tbody>
				</table>
				<div id="shorelines-feature-table-button-sort" class="hidden">
					<select id="ctrl-shorelines-sort-select"></select>
				</div>
			</div>

		</div>
		<div class="tab-pane" id="shorelines-manage-tab">
			<div id="shorelines-uploader" class="uploader"></div>
			<button class="btn btn-success" id="shorelines-triggerbutton"><i class="icon-arrow-up icon-white"></i>Upload</button>
		</div>
	</div>
</div> <!-- /Shorelines -->