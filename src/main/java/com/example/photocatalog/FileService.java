package com.example.photocatalog;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Types;

@Service
@RequiredArgsConstructor
class FileService {

	private final JdbcTemplate jdbcTemplate;

	private final String insertFileSql = " insert into files ( name, path, ext, exists, size_in_bytes, last_modified ) values(?,?,?,?,?,?) "
			+ " ON CONFLICT ON CONSTRAINT path_constraint DO UPDATE SET name=EXCLUDED.path RETURNING id ";

	public Number save(File file) {
		var psc = new PreparedStatementCreatorFactory(this.insertFileSql, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
				Types.BOOLEAN, Types.BIGINT, Types.DATE);
		psc.setGeneratedKeysColumnNames("id");
		psc.setReturnGeneratedKeys(true);
		var params = new Object[] { file.getName(), file.getAbsolutePath(), FileUtils.getExtensionFor(file),
				file.exists(), file.length(), new java.sql.Date(file.lastModified()), };
		var kh = new GeneratedKeyHolder();
		this.jdbcTemplate.update(psc.newPreparedStatementCreator(params), kh);
		return kh.getKey();
	}

}
