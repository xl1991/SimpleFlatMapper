package org.sfm.jdbc.impl.setter;

import org.sfm.reflect.Getter;
import org.sfm.utils.ErrorHelper;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatementIndexSetterOnGetter<I, P> implements PreparedStatementIndexSetter<P> {
    private final Getter<P, I> getter;
    private final PreparedStatementIndexSetter<I> setter;

    public PreparedStatementIndexSetterOnGetter(PreparedStatementIndexSetter<I> setter , Getter<P, I> getter) {
        this.setter = setter;
        this.getter = getter;
    }

    @Override
    public void set(PreparedStatement ps, P value, int columnIndex) throws SQLException {
        try {
            setter.set(ps, getter.get(value), columnIndex);
        } catch (Exception e) {
            ErrorHelper.rethrow(e);
        }
    }
}
