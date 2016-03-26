<!-- Transects --> 
<div class="tab-pane container-fluid" id="transect_verification">
	<div class="row-fluid">
		<div class="span4"><h3>Execution Plan</h3></div>
		<div class="span8" id="transect_verification-alert-container"></div>
	</div>
	<div class="tab-pane active" id="transect_verification-view-tab">
		Calculation Projection: 
		<select id="ctrl-transect-verification-utm">
			<option value="EPSG:26910">UTM 10</option>
			<option value="EPSG:26911">UTM 11</option>
			<option value="EPSG:26912">UTM 12</option>
			<option value="EPSG:26913">UTM 13</option>
			<option value="EPSG:26914">UTM 14</option>
			<option value="EPSG:26915">UTM 15</option>
			<option value="EPSG:26916">UTM 16</option>
			<option value="EPSG:26917">UTM 17</option>
			<option value="EPSG:26918">UTM 18</option>
			<option value="EPSG:26919">UTM 19</option>
		</select>

		<button class="btn btn-success" id="ctrl-transect-verification-submit"><i class="fa fa-cogs icon-white"></i> Calculate</button>
	</div>
</div> <!-- /Transects -->