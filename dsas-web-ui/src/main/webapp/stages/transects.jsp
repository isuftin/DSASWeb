<!-- Transects --> 
<div class="tab-pane container-fluid" id="transects">
	<div class="row-fluid">
		<div class="span4"><h3>Transects</h3></div>
		<div class="span8" id="transects-alert-container"></div>
	</div>
	<ul class="nav nav-tabs" id="action-transects-tablist">
		<li class="active">
			<a  data-toggle="tab" href="#transects-view-tab">View</a>
		</li>
		<li>
			<a data-toggle="tab" href="#transects-manage-tab">Manage</a>
		</li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="transects-view-tab">
			<select id="transects-list" class="feature-list"></select>
		</div>
		<div class="tab-pane" id="transects-manage-tab">
			<div class="row-fluid">
				<div id="transects-uploader" class="uploader"></div>
				<button class="btn btn-success" id="transects-triggerbutton" disabled="disabled"><i class="icon-arrow-up icon-white"></i>Upload</button>
				<button class="btn btn-success" disabled id="transects-downloadbutton"><i class="icon-arrow-down icon-white"></i>Download</button>
				<button data-toggle="button" class="btn btn-success" disabled id="create-transects-toggle">
					<i class="icon-tasks icon-white"></i>
					&nbsp;Generate
				</button>
				<button data-toggle="button" class="btn btn-success" disabled id="transect-edit-form-toggle">
					<i class="icon-edit icon-white"></i>
					&nbsp;Edit
				</button>
			</div>

			<div  id="transects-edit-container" class="row-fluid hidden">
				<div id="transects-update-panel-well" class="well">
					<label for="update-intersections-nearestfarthest-list">Take Nearest/Farthest Intersection</label>
					<select id="update-intersections-nearestfarthest-list" style="width: 100%;">
						<option selected="selected" value="false">Nearest</option>
						<option value="true">Farthest</option>
					</select>
				</div>
				<button class="btn btn-success" id="transects-edit-save-button" title="Update Modified Transect">Update Transects</button>
				<button class="btn btn-success" id="transects-edit-add-button" title="Add Transect" data-toggle="button">Add Transect</button>
				<button class="btn btn-success" id="transects-edit-crop-button" title="Crop Transect" data-toggle="button">Crop Transects</button>
			</div>

			<div id="create-transects-panel-well" class="row-fluid  hidden">
				<div class="well span6">
					Transects
					<div id="create-transects-panel-container">
						<div class="row-fluid">
							<label for="create-transects-input-spacing">Spacing</label>
							<input type="text" id="create-transects-input-spacing" maxLength="6" placeholder="500">m
							<input type="hidden" id="create-transects-input-name" class="customLayerName" style="width: 100%;">
							<label for="create-transects-input-smoothing">Baseline Smoothing</label>
							<input type="text" id="create-transects-input-smoothing" maxLength="6" placeholder="0.0">m
						</div>
					</div>
				</div>
				<div id="intersection-calculation-panel-well" class="well hidden span6">
					Intersections
					<label for="create-intersections-nearestfarthest-list">Take Nearest/Farthest Intersection</label>
					<select id="create-intersections-nearestfarthest-list" style="width: 100%;">
						<option selected="selected" value="false">Nearest</option>
						<option value="true">Farthest</option>
					</select>
				</div>
				<div class="control-group">
					<button type="button" class="btn btn-success span12 hidden" id="create-transects-input-button">
						<i class="icon-tasks icon-white"></i>
						&nbsp;Cast Transects
					</button>
				</div>
			</div>
		</div>
	</div>
</div> <!-- /Transects -->