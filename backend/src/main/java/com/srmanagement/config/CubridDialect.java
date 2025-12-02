package com.srmanagement.config;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * CUBRID 데이터베이스를 위한 커스텀 Dialect.
 * Hibernate 6.x에서 CUBRIDDialect가 기본 제공되지 않거나 경로가 변경된 문제를 해결하기 위해 생성.
 * CUBRID는 MySQL과 문법적으로 유사하므로 MySQLDialect를 상속받아 사용.
 */
public class CubridDialect extends MySQLDialect {
    
    public CubridDialect() {
        super();
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl() {
            @Override
            public boolean supportsIdentityColumns() {
                return true;
            }

            @Override
            public String getIdentitySelectString(String table, String column, int type) {
                return "select last_insert_id()";
            }

            @Override
            public String getIdentityColumnString(int type) {
                return "auto_increment";
            }

            @Override
            public boolean supportsInsertSelectIdentity() {
                return false;
            }

            // Hibernate 6에서 getGeneratedKeys() 사용을 비활성화하기 위해 시도
            // 만약 컴파일 에러가 발생하면 이 메서드는 제거해야 함
            @Override
            public boolean supportsGetGeneratedKeys() {
               return false;
            }
        };
    }

    @Override
    public SequenceSupport getSequenceSupport() {
        return new SequenceSupport() {
            @Override
            public boolean supportsSequences() {
                return true;
            }

            @Override
            public String getSequenceNextValString(String sequenceName) {
                return "select " + sequenceName + ".next_value";
            }

            @Override
            public String getSelectSequenceNextValString(String sequenceName) {
                return "select " + sequenceName + ".next_value";
            }

            @Override
            public String getCreateSequenceString(String sequenceName) {
                return "create serial " + sequenceName;
            }

            @Override
            public String getDropSequenceString(String sequenceName) {
                return "drop serial " + sequenceName;
            }

            @Override
            public String getFromDual() {
                return "";
            }
            
            @Override
            public boolean supportsPooledSequences() {
                return true;
            }
        };
    }
}
