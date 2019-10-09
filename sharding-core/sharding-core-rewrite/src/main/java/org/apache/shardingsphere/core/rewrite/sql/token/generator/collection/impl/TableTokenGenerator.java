/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.rewrite.sql.token.generator.collection.impl;

import com.google.common.base.Optional;
import lombok.Setter;
import org.apache.shardingsphere.core.optimize.segment.table.Table;
import org.apache.shardingsphere.core.optimize.statement.SQLStatementContext;
import org.apache.shardingsphere.core.parse.sql.segment.SQLSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.SelectItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.SelectItemsSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.ShorthandSelectItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.generic.OwnerAvailable;
import org.apache.shardingsphere.core.parse.sql.segment.generic.TableAvailable;
import org.apache.shardingsphere.core.parse.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.rewrite.sql.token.generator.collection.CollectionSQLTokenGenerator;
import org.apache.shardingsphere.core.rewrite.sql.token.generator.ShardingRuleAware;
import org.apache.shardingsphere.core.rewrite.sql.token.pojo.impl.TableToken;
import org.apache.shardingsphere.core.rule.ShardingRule;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Table token generator.
 *
 * @author zhangliang
 * @author panjuan
 */
@Setter
public final class TableTokenGenerator implements CollectionSQLTokenGenerator, ShardingRuleAware {
    
    private ShardingRule shardingRule;
    
    @Override
    public boolean isGenerateSQLToken(final SQLStatementContext sqlStatementContext) {
        return true;
    }
    
    @Override
    public Collection<TableToken> generateSQLTokens(final SQLStatementContext sqlStatementContext) {
        Collection<TableToken> result = new LinkedList<>();
        for (SQLSegment each : sqlStatementContext.getSqlStatement().getAllSQLSegments()) {
            if (each instanceof SelectItemsSegment) {
                result.addAll(createTableTokens(sqlStatementContext, (SelectItemsSegment) each));
            } else if (each instanceof ColumnSegment) {
                Optional<TableToken> tableToken = createTableToken(sqlStatementContext, (ColumnSegment) each);
                if (tableToken.isPresent()) {
                    result.add(tableToken.get());
                }
            } else if (each instanceof TableAvailable) {
                Optional<TableToken> tableToken = createTableToken(sqlStatementContext.getSqlStatement(), (TableAvailable) each);
                if (tableToken.isPresent()) {
                    result.add(tableToken.get());
                }
            }
        }
        return result;
    }
    
    private Collection<TableToken> createTableTokens(final SQLStatementContext sqlStatementContext, final SelectItemsSegment selectItemsSegment) {
        Collection<TableToken> result = new LinkedList<>();
        for (SelectItemSegment each : selectItemsSegment.getSelectItems()) {
            if (each instanceof ShorthandSelectItemSegment) {
                Optional<TableToken> tableToken = createTableToken(sqlStatementContext, (ShorthandSelectItemSegment) each);
                if (tableToken.isPresent()) {
                    result.add(tableToken.get());
                }
            }
        }
        return result;
    }
    
    private Optional<TableToken> createTableToken(final SQLStatementContext sqlStatementContext, final OwnerAvailable<TableSegment> segment) {
        Optional<TableSegment> owner = segment.getOwner();
        if (!owner.isPresent()) {
            return Optional.absent();
        }
        if (isToGenerateTableToken(sqlStatementContext, owner.get())) {
            return Optional.of(new TableToken(owner.get().getStartIndex(), owner.get().getStopIndex(), owner.get().getTableName(), owner.get().getQuoteCharacter()));
        }
        return Optional.absent();
    }
    
    private Optional<TableToken> createTableToken(final SQLStatement sqlStatement, final TableAvailable segment) {
        if (isToGenerateTableToken(sqlStatement, segment)) {
            return Optional.of(new TableToken(segment.getStartIndex(), segment.getStopIndex(), segment.getTableName(), segment.getTableQuoteCharacter()));
        }
        return Optional.absent();
    }
    
    private boolean isToGenerateTableToken(final SQLStatementContext sqlStatementContext, final TableSegment tableSegment) {
        Optional<Table> table = sqlStatementContext.getTablesContext().find(tableSegment.getTableName());
        return table.isPresent() && !table.get().getAlias().isPresent() && shardingRule.findTableRule(table.get().getName()).isPresent();
    }
    
    private boolean isToGenerateTableToken(final SQLStatement sqlStatement, final TableAvailable segment) {
        return shardingRule.findTableRule(segment.getTableName()).isPresent() || !(sqlStatement instanceof SelectStatement);
    }
}
