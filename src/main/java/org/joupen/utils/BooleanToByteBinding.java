package org.joupen.utils;

import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.SQLException;

public class BooleanToByteBinding implements Binding<Byte, Boolean> {

    private static final Converter<Byte, Boolean> CONVERTER = new Converter<>() {
        @Override
        public Boolean from(Byte databaseObject) {
            return databaseObject != null && databaseObject == 1;
        }

        @Override
        public Byte to(Boolean userObject) {
            return userObject != null && userObject ? (byte) 1 : (byte) 0;
        }

        @Override
        public Class<Byte> fromType() {
            return Byte.class;
        }

        @Override
        public Class<Boolean> toType() {
            return Boolean.class;
        }
    };

    @Override
    public Converter<Byte, Boolean> converter() {
        return CONVERTER;
    }

    @Override
    public void sql(BindingSQLContext<Boolean> bindingSQLContext) throws SQLException {
        bindingSQLContext.render().sql("?");
    }

    @Override
    public void register(BindingRegisterContext<Boolean> bindingRegisterContext) throws SQLException {

    }

    @Override
    public void get(BindingGetResultSetContext<Boolean> ctx) throws SQLException {
        ctx.value(converter().from(ctx.resultSet().getByte(ctx.index())));
    }

    @Override
    public void get(BindingGetStatementContext<Boolean> ctx) throws SQLException {
        ctx.value(converter().from(ctx.statement().getByte(ctx.index())));
    }

    @Override
    public void get(BindingGetSQLInputContext<Boolean> bindingGetSQLInputContext) throws SQLException {

    }

    @Override
    public void set(BindingSetStatementContext<Boolean> ctx) throws SQLException {
        ctx.statement().setByte(ctx.index(), converter().to(ctx.value()));
    }

    @Override
    public void set(BindingSetSQLOutputContext<Boolean> bindingSetSQLOutputContext) throws SQLException {

    }
}
