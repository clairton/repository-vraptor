package br.eti.clairton.repository.http;

import static br.eti.clairton.repository.Comparators.EQUAL;
import static br.eti.clairton.repository.Comparators.LIKE;
import static br.eti.clairton.repository.Order.Direction.ASC;
import static br.eti.clairton.repository.Order.Direction.byString;
import static br.eti.clairton.repository.http.Param.DIRECTION;
import static br.eti.clairton.repository.http.Param.PAGE;
import static br.eti.clairton.repository.http.Param.PER_PAGE;
import static br.eti.clairton.repository.http.Param.SORT;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;

import br.eti.clairton.repository.Comparator;
import br.eti.clairton.repository.Order;
import br.eti.clairton.repository.Order.Direction;
import br.eti.clairton.repository.Predicate;

@Dependent
public class QueryParser {
	private static final Logger logger = getLogger(QueryParser.class.getSimpleName());

	private final List<String> query = asList(SORT, PAGE, PER_PAGE, DIRECTION);

	private final AttributeBuilder builder;

	@Deprecated
	protected QueryParser() {
		this(null);
	}

	@Inject
	public QueryParser(final AttributeBuilder builder) {
		super();
		this.builder = builder;
	}

	public Collection<Predicate> parse(final Map<String, String[]> params, final Class<?> modelType) {
		final Collection<Predicate> predicates = new ArrayList<Predicate>();
		final String ids = idsKey(modelType);
		final String id = idKey(modelType);
		if (params.containsKey(ids)) {
			final String[] values;
			if(params.containsKey(id)){
				values = concat(stream(params.get(id)), stream(params.get(ids))).toArray(String[]::new);
			}else{				
				values = params.get(ids);
			}
			params.put(id, values);
			params.remove(ids);
		}
		final Set<String> keys = params.keySet();
		for (final String field : keys) {
			if (query.contains(field) || "format".equals(field)) {
				continue;
			}
			final Collection<Predicate> predicate;
			final Attribute<?, ?>[] attrs = builder.with(modelType, field);
			if (attrs.length == 1 && attrs[0] == null) {
				logger.log(WARNING, "Attribute {}#{} not found", new Object[]{modelType, field});
				continue;
			}
			final String[] values = params.get(field);
			predicate = to(attrs, values);
			predicates.addAll(predicate);
		}
		return predicates;
	}

	public List<Order> order(final Map<String, String[]> params, final Class<?> modelType) {
		final List<Order> orders = new ArrayList<Order>();
		if (params == null) {
			return orders;
		}
		final String[] sort;
		final String[] orderBy;
		if (params.containsKey(DIRECTION)) {
			sort = params.get(DIRECTION);
		} else {
			sort = new String[] { "asc" };
		}
		if (params.containsKey(SORT)) {
			orderBy = params.get(SORT);
		} else {
			orderBy = new String[] {idKey(modelType)};
		}
		for (int i = 0, j = orderBy.length; i < j; i++) {
			final String field = orderBy[i];
			final Attribute<?, ?>[] attrs = builder.with(modelType, field);
			Direction type;
			try {
				type = byString(sort[i]);
			} catch (final ArrayIndexOutOfBoundsException e) {
				type = ASC;
			}
			final Order order = new Order(type, attrs);
			orders.add(order);
		}
		return orders;
	}

	public Page paginate(final Map<String, String[]> params, final Class<?> modelType) {
		if (params == null) {
			return new Page(0, 0);
		}
		final Integer page;
		final Integer perPage;
		if (params.containsKey(PAGE)) {
			page = Integer.valueOf(params.get(PAGE)[0]);
		} else {
			page = 0;
		}
		if (params.containsKey(PER_PAGE)) {
			perPage = Integer.valueOf(params.get(PER_PAGE)[0]);
		} else {
			perPage = 0;
		}
		return new Page(page, perPage);
	}

	protected Record to(final String value) {
		return Record.valueOf(value);
	}

	protected Record to(final String[] values) {
		return Record.valueOf(values);
	}

	protected <T> Predicate to(final Attribute<?, ?>[] attrs, final String value) {
		final Record record = to(value);
		final Attribute<?, ?> attr = attrs[attrs.length - 1];
		final Class<?> javaType;
		if(attr.isCollection()){
			final PluralAttribute<?, ?, ?> pa = (PluralAttribute<?, ?, ?>) attr;
			final Type<?> type = pa.getElementType();
			javaType = type.getJavaType();			
		} else {
			javaType = attr.getJavaType();
		}
		final Object object;
		if(javaType.isEnum()){
			object = parseEnum(record.value, javaType);
		} else if(javaType.isAssignableFrom(Boolean.class)){
			object = parseBoolean(record.value);			
		} else if(javaType.isAssignableFrom(LocalDate.class)){
			object = parseLocalDate(record.value);			
		} else {
			object = record.value.toString();			
		}
		final Comparator comparator = record.comparator;
		final Predicate predicate = new Predicate(object, comparator, attrs);
		return predicate;
	}

	protected <T> Collection<Predicate> to(final Attribute<?, ?>[] attrs, final String[] value) {
		if (value.length > 1) {
			final List<Predicate> predicates = new ArrayList<>();
			for (final String s : value) {
				final Predicate predicate = to(attrs, s);
				predicates.add(predicate);
			}
			// verificar se todas as comparações são iguais, significa que deve
			// ser um like
			if (predicates.stream().filter(p -> p.getComparator().equals(EQUAL)).count() == Long.valueOf(predicates.size())) {
				final Attribute<?, ?>[] attributes = predicates.get(0).getAttributes();
				final List<?> values = predicates.stream().map(p -> p.getValue()).collect(toList());
				final Comparator comparator = LIKE;
				final Predicate predicate = new Predicate(values, comparator, attributes);
				return asList(predicate);
			}
			return predicates;
		} else {
			final Predicate predicate = to(attrs, value[0]);
			return asList(predicate);
		}
	}
	
	protected String idsKey(final Class<?> modelType){
		return "ids[]";
	}
	
	protected String idKey(final Class<?> modelType){
		return "id";
	}
	
	protected Boolean parseBoolean(final Object object){
		return Boolean.valueOf(object.toString());
	}
	
	protected LocalDate parseLocalDate(final Object object){
		final String[] values = object.toString().split(dateSeparator());
		return LocalDate.of(Integer.valueOf(values[0]), Integer.valueOf(values[1]), Integer.valueOf(values[2]));
	}
	
	protected String dateFormat(){
		return "yyyy-mm-dd";
	}
	
	protected String dateSeparator(){
		return "-";
	}
	
	protected Enum<?> parseEnum(final Object object, @SuppressWarnings("rawtypes") final Class type){
		@SuppressWarnings("unchecked")
		final Object e = Enum.valueOf(type, object.toString());
		return (Enum<?>) e;
	}
}
