public class JobseekerPanel extends JPanel {
    private DefaultTableModel jobModel;
    private DefaultTableModel myAppsModel;
    private JTextField searchField;
    private JTable jobsTable;
    private JTable myAppsTable;

    public JobseekerPanel(JobPortalFrame app) {
        setLayout(new BorderLayout());

        // === Title ===
        JLabel title = new JLabel("Jobseeker Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // === Search Bar ===
        JPanel searchBar = new JPanel(new BorderLayout(8, 8));
        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        searchBar.setBorder(BorderFactory.createTitledBorder("Search Jobs"));
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchBtn, BorderLayout.EAST);
        add(searchBar, BorderLayout.SOUTH);

        // === Tabs: Search Results / My Applications ===
        JTabbedPane tabs = new JTabbedPane();

        // Search Results table
        String[] cols = {"ID", "Title", "Company", "Description"};
        jobModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        jobsTable = new JTable(jobModel);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton applyBtn = new JButton("Apply to Selected");
        JPanel searchResultsPanel = new JPanel(new BorderLayout());
        searchResultsPanel.add(new JScrollPane(jobsTable), BorderLayout.CENTER);
        searchResultsPanel.add(applyBtn, BorderLayout.SOUTH);

        tabs.addTab("Search Results", searchResultsPanel);

        // My Applications table
        String[] myCols = {"Job ID", "Title", "Company"};
        myAppsModel = new DefaultTableModel(myCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        myAppsTable = new JTable(myAppsModel);
        JPanel myAppsPanel = new JPanel(new BorderLayout());
        myAppsPanel.add(new JScrollPane(myAppsTable), BorderLayout.CENTER);
        JButton refreshMyAppsBtn = new JButton("Refresh");
        myAppsPanel.add(refreshMyAppsBtn, BorderLayout.SOUTH);
        tabs.addTab("My Applications", myAppsPanel);

        add(tabs, BorderLayout.CENTER);

        // === Populate initial results (all jobs) ===
        fillJobsTable(Store.allJobs());

        // === Listeners ===
        searchBtn.addActionListener(e -> {
            String q = searchField.getText().trim();
            List<Job> results = q.isEmpty() ? Store.allJobs() : Store.searchJobs(q);
            fillJobsTable(results);
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(app, "No jobs found for: " + q);
            }
        });

        applyBtn.addActionListener(e -> {
            int row = jobsTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(app, "Select a job to apply.");
                return;
            }
            User u = app.getCurrentUser();
            if (u == null) {
                JOptionPane.showMessageDialog(app, "Please login first.");
                return;
            }

            int jobId = (Integer) jobModel.getValueAt(row, 0);
            String username = u.getName();

            // Prevent duplicate applications by the same user for the same job
            boolean alreadyApplied = Store.applicationsForJob(jobId).stream()
                    .anyMatch(a -> a.getJobseeker().equals(username));
            if (alreadyApplied) {
                JOptionPane.showMessageDialog(app, "You already applied to this job.");
                return;
            }

            Store.apply(new Application(jobId, username));
            JOptionPane.showMessageDialog(app, "Application submitted!");
            refreshMyApplications(app);
        });

        refreshMyAppsBtn.addActionListener(e -> refreshMyApplications(app));

        // Double-click row to show full description
        jobsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = jobsTable.getSelectedRow();
                    if (r >= 0) {
                        String title = String.valueOf(jobModel.getValueAt(r, 1));
                        String company = String.valueOf(jobModel.getValueAt(r, 2));
                        String desc = String.valueOf(jobModel.getValueAt(r, 3));
                        JOptionPane.showMessageDialog(app,
                                title + " @ " + company + "\n\n" + desc,
                                "Job Details",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // Initial fill of "My Applications" if someone is already logged in
        refreshMyApplications(app);
    }

    private void fillJobsTable(List<Job> jobs) {
        jobModel.setRowCount(0);
        for (Job j : jobs) {
            jobModel.addRow(new Object[]{j.getId(), j.getTitle(), j.getCompany(), j.getDesc()});
        }
    }

    private void refreshMyApplications(JobPortalFrame app) {
        myAppsModel.setRowCount(0);
        User u = app.getCurrentUser();
        if (u == null) return;
        String username = u.getName();

        for (Job j : Store.allJobs()) {
            boolean userAppliedForThisJob = Store.applicationsForJob(j.getId()).stream()
                    .anyMatch(a -> a.getJobseeker().equals(username));
            if (userAppliedForThisJob) {
                myAppsModel.addRow(new Object[]{j.getId(), j.getTitle(), j.getCompany()});
            }
        }
    }
}