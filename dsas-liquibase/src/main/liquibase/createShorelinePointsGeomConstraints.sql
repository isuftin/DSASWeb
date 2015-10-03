--add constraint to enforce geometry type
ALTER TABLE points_table ADD CONSTRAINT enforce_geotype_the_geom_4326 CHECK (geometrytype(the_geom_4326) = 'POINT'::text OR the_geom_4326 IS NULL);

-- add constraint to enforce number of dimensions
ALTER TABLE myschema.points_table ADD CONSTRAINT enforce_dims_the_geom_4326 CHECK (st_ndims(the_geom_4326) = 2);

-- add constraint to enforce SRS
ALTER TABLE myschema.points_table ADD CONSTRAINT enforce_srid_the_geom_4326 CHECK (st_srid(the_geom_4326) = 4326);
