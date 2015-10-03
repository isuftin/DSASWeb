<!-- Calculation -->
<div class="tab-pane  container-fluid" id="calculation">
	<div class="row-fluid">
		<div class="span4"><h3>Review/Calculate</h3></div>
		<div class="span8" id="calculation-alert-container"></div>
	</div>
	<ul class="nav nav-tabs" id="action-intersections-tablist">
		<li class="active"><a  data-toggle="tab" href="#intersections-view-tab">View</a></li>
		<li><a data-toggle="tab" href="#intersections-manage-tab">Manage</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="intersections-view-tab">
			<select id="intersections-list" class="feature-list"></select>
		</div>
		<div class="tab-pane" id="intersections-manage-tab">
			<!-- Intersection Calculation -->
			<div class="row-fluid">
				<div id="results-calculation-panel-well" class="well span6">
					<input type="hidden" class="input-large" name="results-form-name"  id="results-form-name"  style="width: 100%;" />
					<label class="control-label" for="results-form-ci">Confidence Interval</label>
					<input type="number" min="50" max="100" step="1" value="90" class="input-large" name="results-form-ci" id="results-form-ci"  style="width: 50%;">% (50-100)
					<button class="btn btn-success span12" id="create-results-btn">
						<i class="icon-tasks icon-white"></i>
						&nbsp;Calculate Results
					</button>
				</div>
				<div id="intersections-management-panel-well" class="well span6">
					<button class="btn btn-success" disabled id="intersections-downloadbutton"><i class="icon-arrow-down icon-white"></i>Download</button>
				</div>
			</div>
		</div>
	</div>
</div> <!-- /Calculation -->