explain (format text, analyze, buffers on, memory on, settings on, timing on, verbose on, wal on)
SELECT c._cellline_key,
    c.cellline,
    c._cellline_type_key,
    c._strain_key,
    c._derivation_key,
    c.ismutant,
    c._createdby_key,
    c._modifiedby_key,
    c.creation_date,
    c.modification_date,
    vt.term AS celllinetype,
    s1.strain AS celllinestrain,
    d._creator_key,
    vt1.term AS creator,
    p._cellline_key AS parentcellline_key,
    p.cellline AS parentcellline,
    d.name AS derivationname,
    d._derivationtype_key,
    d._vector_key,
    vt2.term AS vector,
    d._vectortype_key,
    vt3.term AS vectortype,
    p._strain_key AS parentcelllinestrain_key,
    s2.strain AS parentcelllinestrain,
    u1.login AS createdby,
    u2.login AS modifiedby
   FROM mgd.all_cellline c
     JOIN mgd.voc_term vt ON c._cellline_type_key = vt._term_key
     JOIN mgd.prb_strain s1 ON c._strain_key = s1._strain_key
     JOIN mgd.mgi_user u1 ON c._createdby_key = u1._user_key
     JOIN mgd.mgi_user u2 ON c._modifiedby_key = u2._user_key
     LEFT JOIN mgd.all_cellline_derivation d ON c._derivation_key = d._derivation_key
     LEFT JOIN mgd.voc_term vt1 ON d._creator_key = vt1._term_key
     LEFT JOIN mgd.voc_term vt2 ON d._vector_key = vt2._term_key
     LEFT JOIN mgd.voc_term vt3 ON d._vectortype_key = vt3._term_key
     LEFT JOIN mgd.all_cellline p ON d._parentcellline_key = p._cellline_key
     LEFT JOIN mgd.prb_strain s2 ON p._strain_key = s2._strain_key
