<!-- Results -->
<div class="tab-pane container-fluid" id="results">
	<div class="row-fluid">
		<div class="span4"><h3>Results</h3></div>
		<div class="span8" id="results-alert-container"></div>
	</div>
	<ul class="nav nav-tabs" id="action-result-tablist">
		<li class="active"><a  data-toggle="tab" href="#results-view-tab">View</a></li>
		<li><a data-toggle="tab" href="#results-manage-tab">Manage</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="results-view-tab">
			<select id="results-list" class="feature-list"></select>
			<div class="row-fluid">
				<div class="tabbable">
					<ul class="nav nav-tabs" id="results-table-navtabs"></ul>
					<div class="tab-content" id="results-tabcontent"></div>
				</div>
			</div>
		</div>
		<div class="tab-pane" id="results-manage-tab">
			<h4>Download Results</h4>
			<p>Your browser's popup blocker might attempt to block these downloads. Direct your browser to allow popups for this site to streamline your data export experience.</p>
			<button class="btn btn-success" id="download-plot-btn" disabled>
				<i class="icon-signal icon-white"></i>
				&nbsp;Plot (.png)
			</button>
			<button class="btn btn-success" id="download-spreadsheet-btn" disabled>
				<i class="icon-th icon-white"></i>
				&nbsp;Spreadsheet (.csv)
			</button>
			<button class="btn btn-success" id="download-shapefile-btn" disabled>
				<i class="icon-file icon-white"></i>
				&nbsp;Shapefile (.zip)
			</button>

		</div>
	</div>
</div> <!-- /Results -->