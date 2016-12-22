/*
 * Copyright 2012 Johann Gyger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.gyger.jmoney.account;

import name.gyger.jmoney.category.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntryService {

    private final AccountService accountService;

    @PersistenceContext
    private EntityManager em;

    public EntryService(AccountService accountService) {
        this.accountService = accountService;
    }

    public long getEntryCount(long accountId) {
        Query q = em.createQuery("SELECT count(e) FROM Entry e WHERE e.account.id = :id");
        q.setParameter("id", accountId);
        return (Long) q.getSingleResult();
    }

    public List<Entry> getEntries(long accountId, Integer page, String filter) {
        TypedQuery<Entry> q = em.createQuery("SELECT e FROM Entry e LEFT JOIN FETCH e.category WHERE e.account.id = :id" +
                " ORDER BY CASE WHEN e.date IS NULL THEN 1 ELSE 0 END, e.date, e.creation", Entry.class);
        q.setParameter("id", accountId);

        final long[] balance = {accountService.getAccount(accountId).getStartBalance()};
        List<Entry> entries = q.getResultList().stream()
                .filter(e -> e.contains(filter))
                .map(e -> {
                    balance[0] += e.getAmount();
                    e.setBalance(balance[0]);
                    return e;
                })
                .collect(Collectors.toList());

        Collections.reverse(entries);

        if (page == null) {
            page = 1;
        }
        int count = entries.size();
        int from = Math.min((page - 1) * 10, count);
        int to = Math.min((page - 1) * 10 + 10, count);
        entries = entries.subList(from, to);

        entries.forEach(e -> em.detach(e));

        return entries;
    }

    public Entry getEntry(long id) {
        Entry entry = em.find(Entry.class, id);

        Account account = entry.getAccount();
        if (account != null) {
            entry.setAccountId(account.getId());
        }

        Category category = entry.getCategory();
        if (category != null) {
            entry.setCategoryId(category.getId());
        }

        em.detach(entry);

        return entry;
    }

    public long createEntry(Entry entry) {
        em.detach(entry);
        entry.setId(0);
        em.persist(entry);
        updateEntryInternal(entry);
        entry.setStatus(Entry.Status.CLEARED); // TODO: What is this?
        return entry.getId();
    }

    public void updateEntry(Entry entry) {
        updateEntryInternal(entry);
        em.merge(entry);
    }

    private void updateEntryInternal(Entry e) {
        Account a = em.find(Account.class, e.getAccountId());
        e.setAccount(a);

        Category c = em.find(Category.class, e.getCategoryId());
        e.setCategory(c);

        removeOldSubEntries(e);
        if (c != null && c.getType() == Category.Type.SPLIT) {
            createSubEntries(e);
        }

        if (c instanceof Account) {
            Account otherAccount = (Account) c;

            Entry other = e.getOther();
            if (other == null) {
                other = new Entry();
                em.persist(other);
            }

            other.setOther(e);
            other.setCategory(a);
            other.setAccount(otherAccount);
            e.setOther(other);
        } else {
            Entry other = e.getOther();
            e.setOther(null);

            if (other != null) {
                other.setOther(null);
                em.remove(other);
            }
        }
    }

    private void removeOldSubEntries(Entry e) {
        TypedQuery<Entry> q = em.createQuery("SELECT e FROM Entry e WHERE e.splitEntry.id = :id", Entry.class);
        q.setParameter("id", e.getId());
        List<Entry> subEntries = q.getResultList();
        subEntries.forEach(subEntry -> em.remove(subEntry));
    }

    private void createSubEntries(Entry e) {
        List<Entry> subEntries = e.getSubEntries();
        for (Entry subEntry : subEntries) {
            em.persist(subEntry);
            Category subCat = em.find(Category.class, subEntry.getCategoryId());
            subEntry.setCategory(subCat);
            subEntry.setSplitEntry(e);
        }
    }

    public void deleteEntry(long entryId) {
        Entry e = em.find(Entry.class, entryId);
        em.remove(e);
    }
}