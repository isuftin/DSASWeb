package gov.usgs.cida.dsas.dao;

import gov.usgs.cida.dsas.dao.pdb.Pdb;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class DaoQueryBuilderTest {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DaoQueryBuilderTest.class);

	@Test
	public void testBuildPdbQueryString() {
		LOGGER.info("testBuildPdbQueryString");
		StringBuilder sql = new StringBuilder();
		StringBuilder values = new StringBuilder();
		Timestamp now = getUTCNowAsSQLTimestamp();

		List<Pdb> pdbs = getPdbs();

		for (Pdb pdb : pdbs) {
			values.append("( ")
					.append(pdb.getProfileId())  //int
					.append(",")
					.append(pdb.getSegmentId())  //BigInt
					.append(",")
					.append("ST_GeomFromText('POINT(")
					.append(pdb.getX()) //  ... ST_GeomFromText('POINT(x y)',4326)
					.append(" ")
					.append(pdb.getY())
					.append(")',")
					.append(FeatureTypeFileDAO.DATABASE_PROJECTION)
					.append("),")
					.append(pdb.getBias())  //double
					.append(",")
					.append(pdb.getUncyb())  //double
					.append(",")
					.append("to_timestamp('")
					.append(now)
					.append("', 'YYYY-MM-DD HH24:MI:SS.MS')")
					.append("),");
		}
		assertNotNull(values);
		LOGGER.info(values.toString());
		values.deleteCharAt(values.length() - 1);

		sql.append("WITH new_vals (profile_id, segment_id, xy, bias, uncyb, last_update) AS (")
				.append("values ")
				.append(values)
				.append("), upsert AS (UPDATE proxy_datum_bias p set ")
				.append("profile_id = nv.profile_id, ")
				.append("segment_id = nv.segment_id, ")
				.append("xy = nv.xy, ")
				.append("bias = nv.bias, ")
				.append("uncyb = nv.uncyb, ")
				.append("last_update = nv.last_update ")
				.append("FROM new_vals as nv WHERE nv.profile_id = p.profile_id RETURNING p.*) ")
				.append("INSERT INTO proxy_datum_bias (profile_id, segment_id, xy, bias, uncyb, last_update) ")
				.append("SELECT profile_id, segment_id, xy, bias, uncyb, last_update ")
				.append("FROM new_vals WHERE NOT EXISTS (SELECT 1 FROM upsert WHERE upsert.profile_id = new_vals.profile_id)");

		LOGGER.info("Resulting insert points string is: " + sql.toString());

	}

	private Timestamp getUTCNowAsSQLTimestamp() {
		Instant now = Instant.now();
		java.sql.Timestamp currentTimestamp = Timestamp.from(now);
		return currentTimestamp;
	}

	//mock a bunch of Pdbs for the test
	private ArrayList<Pdb> getPdbs() {
		ArrayList<Pdb> pdbList = new ArrayList();

		for (int i = 1; i < 3; i++) {
			Pdb pdb = new Pdb();
			int segmentId = i;
			pdb.setSegmentId(BigInteger.valueOf(segmentId));

			int profileId = i;
			pdb.setProfileId(profileId);

			double bias = i;
			pdb.setBias(bias);

			double biasUncy = i;
			pdb.setUncyb(biasUncy);

			pdbList.add(pdb);
		}

		return pdbList;
	}
}
