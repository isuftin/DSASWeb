<!-- Baseline -->
<div class="tab-pane container-fluid" id="baseline">
	<div class="row-fluid">
		<div class="span4"><h3>Baseline</h3></div>
		<div class="span8" id="baseline-alert-container"></div>
	</div>
	<ul class="nav nav-tabs" id="action-baseline-tablist">
		<li class="active"><a data-toggle="tab" href="#baseline-view-tab">View</a></li>
		<li><a data-toggle="tab" href="#baseline-manage-tab">Manage</a></li>
	</ul>
	<div id="baseline-tab-content" class="tab-content">
		<div class="tab-pane active" id="baseline-view-tab">
			<select id="baseline-list" class="feature-list"></select>
		</div>
		<div class="tab-pane" id="baseline-manage-tab">

			<div id="baseline-button-row" class="row-fluid">
				<div class="row-fluid">
					<div id="baseline-uploader" class="uploader"></div>
					<button class="btn btn-success" id="baseline-triggerbutton"><i class="icon-arrow-up icon-white"></i>Upload</button>
					<button id="baseline-draw-btn" class="btn btn-success" data-toggle="button">
						<i class="icon-pencil icon-white"></i>
						&nbsp;Draw
					</button>
					<div id="baseline-edit-btn-group" class="btn-group">
						<button id="baseline-edit-button" data-toggle="button" class="btn btn-success"  disabled="disabled">
							<i class="icon-edit icon-white"></i>
							&nbsp;Edit
						</button>
						<button id="baseline-edit-form-toggle" class="btn dropdown-toggle btn-success" data-toggle="dropdown" disabled="disabled">
							<span class="caret"></span>
						</button>
						<ul id="baseline-edit-menu" class="dropdown-menu"  role="menu" aria-labelledby="dropdownMenu">
							<li id="baseline-edit-create-vertex"><a tabindex="-1" href="#">Create Vertex</a></li>
							<li id="baseline-edit-rotate"><a tabindex="-1" href="#">Rotate</a></li>
							<li id="baseline-edit-resize"><a tabindex="-1" href="#">Resize</a></li>
							<li id="baseline-edit-resize-w-aspect"><a tabindex="-1" href="#">Resize + Maintain Aspect Ratio</a></li>
							<li id="baseline-edit-drag"><a tabindex="-1" href="#">Drag</a></li>
							<li id="baseline-edit-orient-seaward" class="disabled"><a tabindex="-1" href="#">Set Direction Seaward</a></li>
							<li id="baseline-edit-orient-shoreward" class="disabled"><a tabindex="-1" href="#">Set Direction Shoreward</a></li>
						</ul>
					</div>
					<button class="btn btn-success" disabled id="baseline-clone-btn">
						<i class="icon-plus icon-white"></i>
						&nbsp;Clone
					</button>
					<button class="btn btn-success" disabled id="baseline-downloadbutton"><i class="icon-arrow-down icon-white"></i>Download</button>
					<button id="baseline-remove-btn" disabled class="btn btn-success">
						<i class="icon-remove icon-white"></i>
						&nbsp;Remove
					</button>
				</div>
			</div>

			<!-- Baseline Drawing -->
			<div class="row-fluid">
				<div id="draw-panel-well" class="well hidden">
					<div id="draw-panel-container" class="container-fluid">
						<div class="row-fluid">
							<label class="control-label" for="baseline-draw-form-name">Baseline Name</label>
							<input class="input-large span5" name="baseline-draw-form-name" id="baseline-draw-form-name">
						</div>
						<div class="row-fluid">
							<button class="btn btn-success span2" id="baseline-draw-form-save">Save</button>
							<button class="btn btn-success span2" id="baseline-draw-form-clear">Clear</button>
						</div>
					</div>
				</div>
			</div>

			<!-- Baseline Editing -->
			<div class="row-fluid">
				<div id="baseline-edit-container" class="well hidden">
					<div id="baseline-edit-container-instructions-initial" class="baseline-edit-container-instructions hidden">
						Begin by selecting a base line segment you wish to edit. Select a base line segment by hovering over a segment until it turns blue, then click on it. <br /><br />
						You may also begin adding segments to the baseline by clicking on an empty area on the map to begin drawing.
					</div>
					<div id="baseline-edit-container-instructions-vertex" class="baseline-edit-container-instructions hidden">
						When editing vertices, you have control over two typs of vertices. The vertices appearing at the endpoints and bends of features allow you to drag these endpoints and bends. The less opaque vertices appearing at the midpoint of each segment allow you to cerate new segments by dragging on them.
					</div>
					<div id="baseline-edit-container-instructions-rotate" class="baseline-edit-container-instructions hidden">
						By dragging the single handler for a feature, you are able to rotate the feature around a central point.
					</div>
					<div id="baseline-edit-container-instructions-resize" class="baseline-edit-container-instructions hidden">
						Drag the handler to resize the selected feature.  If you wish, you are also able to maintain the feature's aspect ratio while resizing.
					</div>
					<div id="baseline-edit-container-instructions-drag" class="baseline-edit-container-instructions hidden">
						Drag the handler to drag the selected feature.
					</div>
					<div class="row-fluid">
						<button class="btn btn-success" id="baseline-edit-save-button" title="Update Modified Baseline">Update Baseline</button>
					</div>
				</div>

			</div>
		</div>
	</div>
</div> <!-- /Baseline -->